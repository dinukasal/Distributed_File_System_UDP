package Node.FileSearch;

import Node.Communicators.SearchCommunicator;
import Node.Communicators.SearchCommunicatorUDPImpl;
import Node.Node;
import Node.Neighbour;
import Node.FileWrite;
import Node.PerformanceEvaluator;

import com.sun.org.apache.xpath.internal.SourceTree;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by nadunindunil on 11/17/17.
 */
public class FileSearchImpl implements FileSearch {
    private HashMap<String, File> filesToStore = new HashMap<>();

    //to keep extra info about files stored in other nodes
    private HashMap<String, String[]> searchedResults = new HashMap<>();
    //to keep records of successful seaches
    private ArrayList<String[]> searchRecords = new ArrayList<>();
    private ArrayList<String[]> searchResultsToDisplay;
    private SearchCommunicator searchCommunicator = new SearchCommunicatorUDPImpl();
    private Node node;
    private ExecutorService executorService;
    private FileWrite fileWriter=new FileWrite();
    private int waiting_time=1000;
    private int hops=0;
    private int MAX_HOPS=10;
    private PerformanceEvaluator perfEval =new PerformanceEvaluator();

    private int fileFoundState=2;
    long startTime;

    public FileSearchImpl(Node node) {
        this.node = node;

    }

    @Override
    public void start(){

        initializeFiles();

        executorService = Executors.newCachedThreadPool();
        executorService.submit(() -> {
            try {
                readStdin();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    @Override
    public void searchProcess(int hops, String fileName, String originatorIP, int originatorPort) throws IOException {
        ArrayList<String> searchResults = searchFiles(fileName); //search file in the node
        hops--;
        if (searchResults.size() > 0) {
            System.out.println("File Found!!!");
            fileFoundState=1;
            addRecords(fileName, originatorIP, String.valueOf(originatorPort));
            forwardFileSearchOKResponse(searchResults, hops, originatorIP, originatorPort);

        } else {
            if (hops > 0) {
                //check whether filename is already included in previous search results
                String[] ownersDetailsOfFiles = searchPreviousSearchResults(fileName);
                if (ownersDetailsOfFiles != null) {
                    //forward request to owner
                    System.out.println("File found from previous searched results. Request is forwarded directly to the owner.");
                    forwardFileSearchRequestToOwner(fileName, hops, ownersDetailsOfFiles[0], Integer.parseInt(ownersDetailsOfFiles[1]), originatorIP, originatorPort);
                } else {
                    forwardFileSearchRequest(fileName, hops, originatorIP, originatorPort); //forward request to a neighbour
                }
            } else {
                Long elapsed;
                elapsed=(System.nanoTime()-startTime);

                forwardFileSearchOKResponse(null, -10, originatorIP, originatorPort);
                System.out.println("File Couldn't found!!! "+elapsed);
            }
        }
        
    }

    @Override
    public void searchOKProcess(int numberOfHops,List<String> searchResults, String ownerIP, int ownerPort){

        if (numberOfHops != -10) {
            System.out.println("File Found in....");
            System.out.println("Owner IP : " + ownerIP);
            System.out.println("Owner Port : " + ownerPort);
            System.out.println("Number of Hops : " + (MAX_HOPS-numberOfHops));
            hops=MAX_HOPS-numberOfHops;

            ArrayList<String[]> searchResultsToDisplay = new ArrayList<String[]>();
            fileFoundState=1;            

            Long elapsed;
            elapsed=(System.nanoTime()-startTime);

            System.out.println("Files Found-----------------"+elapsed);
            //fileWriter.addLine("1,"+hops+","+Long.toString(elapsed));

            for (String searchResult : searchResults) {
                System.out.println(searchResult);
                searchResultsToDisplay.add(new String[]{searchResult, ownerIP, String.valueOf(ownerPort)});
                saveSearchedResults(searchResult, ownerIP, ownerPort); //save searched results future lookups
            }

            setSearchResultsToDisplay(searchResultsToDisplay);
        }

        if (numberOfHops == -10 && searchResults == null) {
            System.out.println("File not found");
            fileFoundState=0;
        }
    }

    //remove previous search results when graceful departure
    @Override
    public void removeSearchResults(String ip, String port) {
        System.out.println("Remove records request arrived" + ip + " " + port);
        for (String fileNames : searchedResults.keySet()) {
            String[] ownerDetails = searchedResults.get(fileNames);
            if ((ownerDetails[0].equals(ip)) && (ownerDetails[1].equals(port))) {
                searchedResults.remove(fileNames);
            }
        }
    }

    @Override
    public void gracefulDeparture() throws IOException {
        for (String[] details : searchRecords) {
            System.out.println(details[1]+" "+details[2]);
            Neighbour n = new Neighbour(details[1], Integer.parseInt(details[2]), (float) 1.0);
            System.out.println("Remove records request "+details[0]+" "+details[1]);

            searchCommunicator.removeSearchRecords(node.getIpAddress(),node.getPort(),n.getIp(),n.getPort());
        }
    }

    public HashMap<String, File> getFilesToStore() {
        return filesToStore;
    }

    public void readStdin() { //get input from command line -   STDIn handler
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (true) {
                String outMessage = stdin.readLine();

                if (outMessage.contains("ser")) {
                    search(outMessage);
                } else if(outMessage.contains("msg")){  //give msg count
                    System.out.println(node.getMsgCount());
                }else if(outMessage.contains("nbr")){   //count neighbours
                    System.out.println(node.myNeighbourCount());
                }
                else if(outMessage.contains("perf")){   //performance evaluation
                    perfEval.perfEval(node);
                    fileWriter.printLines();
                }
                else {
                    System.out.println("null in this node");
                }
            }
        }  catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void search(String outMessage){  // search result catch  ####################################################################
        try {
            fileFoundState=2;
            startTime=System.nanoTime();
                ArrayList<String> searchResults = searchFiles(outMessage.split(" ")[1]); //search file in the own directory
            if (searchResults.size() > 0) {
                System.out.println("File Found in My Node");
                hops=0;
                fileFoundState=1;
            } else {
                //check whether filename is already included in previous search results
                String[] ownersDetailsOfFiles = searchPreviousSearchResults(outMessage.split(" ")[1]);
                ownersDetailsOfFiles=null;
                if (ownersDetailsOfFiles != null) {
                    //forward request to owner
                    System.out.println("File found from previous searched results. Request is forwarded directly to the owner.");
                    forwardFileSearchRequestToOwner(outMessage.split(" ")[1], MAX_HOPS, ownersDetailsOfFiles[0],
                            Integer.parseInt(ownersDetailsOfFiles[1]), node.getIpAddress(), node.getPort());
                } else {
                    forwardFileSearchRequest(outMessage.split(" ")[1], MAX_HOPS, node.getIpAddress(), node.getPort()); //forward request to a neighbour
                }
            }

            long elapsed=0;

            while(fileFoundState==2){
                elapsed=(System.nanoTime()-startTime);

                if(elapsed/1000000>waiting_time){
                    System.out.println("not found.. Time Elapsed:"+Long.toString(elapsed));
                    //fileWriter.addLine("-1,"+hops+","+Long.toString(elapsed));
                    break;
                }else{
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            }

            if(fileFoundState==1){
                System.out.println("*******File Found Time Elapsed:"+Long.toString(elapsed));
                fileWriter.addLine("1,"+hops+","+Long.toString(elapsed));
                System.out.println("__________________1,"+hops+","+Long.toString(elapsed));
            }else if(fileFoundState==0){
                System.out.println("not found.. Time Elapsed:"+Long.toString(elapsed));
                fileWriter.addLine("0,"+hops+","+Long.toString(elapsed));
                System.out.println("______0_____0_____,"+hops+","+Long.toString(elapsed));
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void writeResults(){
        fileWriter.closeFile();
    }

    @Override
    public void searchWithOk(String outMessage){
        try {
                ArrayList<String> searchResults = searchFiles(outMessage.split(" ")[1]); //search file in the own directory
            if (searchResults.size() > 0) {
                System.out.println("File Found in My Node");
            } else {
                //check whether filename is already included in previous search results
                String[] ownersDetailsOfFiles = searchPreviousSearchResults(outMessage.split(" ")[1]);
                if (ownersDetailsOfFiles != null) {
                    //forward request to owner
                    System.out.println("File found from previous searched results. Request is forwarded directly to the owner.");
                    forwardFileSearchRequestToOwner(outMessage.split(" ")[1], 3, ownersDetailsOfFiles[0],
                            Integer.parseInt(ownersDetailsOfFiles[1]), node.getIpAddress(), node.getPort());
                } else {
                    forwardFileSearchRequest(outMessage.split(" ")[1], 3, node.getIpAddress(), node.getPort()); //forward request to a neighbour
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //Randomly pick two files from the file list.
    private void initializeFiles() {

        HashMap<String, File> allFiles = new HashMap<String, File>();
        // allFiles.put("Lord of the_Rings", new File("G:\\Films\\LR\\Lord_of_the_Rings.mov"));
        // allFiles.put("Harry Porter 1", new File("G:\\Films\\HP\\Harry_Porter_1.mov"));
        // allFiles.put("Fast_and_Furious", new File("G:\\Films\\FF\\Fast_and_Furious.mov"));
        // allFiles.put("La_La_Land", new File("G:\\Films\\LR\\La_La_Land.mov"));
        // allFiles.put("Transformers", new File("G:\\Films\\Transformers\\Transformers.mov"));
        // allFiles.put("Spider_Man_1", new File("G:\\Films\\SP\\Spider_Man_1.mov"));
        // allFiles.put("abc", new File("G:\\Films\\abc\\abc.mov"));

        // The name of the file to open.
        String fileName = "FileNames.txt";

        // This will reference one line at a time
        String line;

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(fileName).getFile());

            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(file);
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                //System.out.println(line);
                allFiles.put(line,new File(line));
            }

            // Always close files.
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            fileName + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + fileName + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }


        //generate 3 random indices to pick files from hashmap
        int[] randomIndices = new Random().ints(1, allFiles.size()).distinct().limit(3).toArray();

        System.out.println("Initiated Files-----------------------");
        //pick files randomly
        ArrayList<String> keysAsArray = new ArrayList<String>(allFiles.keySet());
        for (int fileIndex : randomIndices) {
            filesToStore.put(keysAsArray.get(fileIndex), allFiles.get(keysAsArray.get(fileIndex)));
            System.out.println(keysAsArray.get(fileIndex));
        }
        System.out.println("End Initiated Files-----------------------");

    }

    private ArrayList<String> searchFiles(String fileNameToSearch) {
        ArrayList<String> searchResults = new ArrayList<String>();

        for (String fileNames : filesToStore.keySet()) {
            if (fileNames.contains(fileNameToSearch)) {
                searchResults.add(fileNames);
            }
        }

        return searchResults;
    }

    private String[] searchPreviousSearchResults(String fileNameToSearch) {
        //search files in previous search results
        String[] ownerDetails = null;

        for (String fileNames : searchedResults.keySet()) {
            if (fileNames.contains(fileNameToSearch)) {
                ownerDetails = searchedResults.get(fileNames);
            }
        }

        return ownerDetails;
    }

    //send to owner of files to double check the existence of the file
    private void forwardFileSearchRequestToOwner(String fileNameToSearch, int hops, String ownerIP, int ownerPort,
                                                 String originatorIP, int originatorPort) throws IOException {

        searchCommunicator.search(node.getIpAddress(),node.getPort(),ownerIP,ownerPort,originatorIP,originatorPort,
                fileNameToSearch,hops);
    }

    //send file search request to neighbours
    private void forwardFileSearchRequest(String fileNameToSearch, int hops, String originatorIP, int originatorPort) throws IOException {
        //select random neighbour to forward request
        Random r = new Random();
        Neighbour randomSuccessor = null;
        int totalIterations = 0;

        List<Neighbour> MyNeighbours = node.getMyNeighbours();

        if (MyNeighbours.size() != 0){
            while (true) {
                randomSuccessor = MyNeighbours.get(r.nextInt(MyNeighbours.size()));
                //check whether selected node is equal to myself.
                //TODO: check the ip also
                if (((randomSuccessor.getPort() != node.getPort()) && (randomSuccessor.getProbability() > 0.4)) || ((randomSuccessor
                        .getPort() != node.getPort()) && (totalIterations >= MyNeighbours.size()))) {
                    break;
                }
                totalIterations++;
            }
            Long elapsed=(System.nanoTime()-startTime);
            System.out.println("File Couldn't found & File Search Request forwarded "+elapsed);
            fileWriter.addLine("0,"+hops+","+Long.toString(elapsed));
            

            searchCommunicator.search(node.getIpAddress(),node.getPort(),randomSuccessor.getIp(),
                    randomSuccessor.getPort(),originatorIP,originatorPort,
                    fileNameToSearch,hops);
            System.out.println("rnd :" + randomSuccessor.getIp() +" , " + randomSuccessor.getPort());
        }
        else{
            System.out.println("no neighbours for this node !!!");
        }


    }


    //send search results to query originator
    private void forwardFileSearchOKResponse(ArrayList<String> searchResults, int hops, String originatorIP, int originatorPort) throws IOException {
        Random r = new Random();
        Neighbour randomSuccessor;

        List<Neighbour> MyNeighbours = node.getMyNeighbours();

        while (true) {
            randomSuccessor = MyNeighbours.get(r.nextInt(MyNeighbours.size()));
            //check whether selected node is equal to myself.
            //TODO: check the ip also
            if (randomSuccessor.getPort() != node.getPort()) {
                break;
            }
        }
//        System.out.println(originatorIP + " " + originatorPort);
//        System.out.println(randomSuccessor.getIp() + " " + randomSuccessor.getPort());


        int size = 0;
        if (searchResults != null){
            size = searchResults.size();
        }

        searchCommunicator.searchOK(node.getIpAddress(),node.getPort(),originatorIP,originatorPort,node.getIpAddress
                (),node.getPort(),searchResults,size,hops);
    }

    private void saveSearchedResults(String filename, String ip, int port) {
        ArrayList<String> keysAsArray = new ArrayList<String>(searchedResults.keySet());
        boolean isExist = false;

        //check whether filename exists already ;
        for (String key : keysAsArray) {
            if (key.equals(filename)) {
                isExist = true;
            }
        }

        //save search results
        if (!isExist) {
            searchedResults.put(filename, new String[]{ip, String.valueOf(port)});
        }

    }

    private void setSearchResultsToDisplay(ArrayList<String[]> searchResultsToDisplay) {
        this.searchResultsToDisplay = searchResultsToDisplay;
    }

    //add successful search records
    private void addRecords(String fileName, String originatorIP, String originatorPort) {
        searchRecords.add(new String[]{fileName, originatorIP, originatorPort});
    }

}
