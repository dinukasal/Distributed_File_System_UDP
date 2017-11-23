package Node.FileSearch;


import Node.Communicators.SearchCommunicator;
import Node.Communicators.SearchCommunicatorUDPImpl;
import Node.Node;
import Node.Neighbour;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                forwardFileSearchOKResponse(null, -10, originatorIP, originatorPort);
                System.out.println("File Couldn't found!!!");
            }
        }
    }

    @Override
    public void searchOKProcess(int numberOfHops,List<String> searchResults, String ownerIP, int ownerPort){

        if (numberOfHops != -10) {
            System.out.println("File Found in....");
            System.out.println("Owner IP : " + ownerIP);
            System.out.println("Owner Port : " + ownerPort);
            System.out.println("Number of Hops : " + (3 - numberOfHops));

            ArrayList<String[]> searchResultsToDisplay = new ArrayList<String[]>();

            System.out.println("Files Found-----------------");
            for (String searchResult : searchResults) {
                System.out.println(searchResult);
                searchResultsToDisplay.add(new String[]{searchResult, ownerIP, String.valueOf(ownerPort)});
                saveSearchedResults(searchResult, ownerIP, ownerPort); //save searched results future lookups
            }

            setSearchResultsToDisplay(searchResultsToDisplay);
        }

        if (numberOfHops == -10 && searchResults == null) {
            System.out.println("File not found");
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

    public void readStdin() { //get input from command line
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (true) {
                String outMessage = stdin.readLine();

                if (outMessage.contains("ser")) {
                    search(outMessage);
                } else {
                    System.out.println("null in this node");
                }
            }
        }  catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void search(String outMessage){
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
        allFiles.put("Lord of the_Rings", new File("G:\\Films\\LR\\Lord_of_the_Rings.mov"));
        allFiles.put("Harry Porter 1", new File("G:\\Films\\HP\\Harry_Porter_1.mov"));
        allFiles.put("Fast_and_Furious", new File("G:\\Films\\FF\\Fast_and_Furious.mov"));
        allFiles.put("La_La_Land", new File("G:\\Films\\LR\\La_La_Land.mov"));
        allFiles.put("Transformers", new File("G:\\Films\\Transformers\\Transformers.mov"));
        allFiles.put("Spider_Man_1", new File("G:\\Films\\SP\\Spider_Man_1.mov"));
        allFiles.put("abc", new File("G:\\Films\\abc\\abc.mov"));

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
            System.out.println("File Couldn't found & File Search Request forwarded");

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
