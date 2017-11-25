package Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by nadunindunil on 11/16/17.
 */
public class PerformanceEvaluator {

//    public static void main(String[] args) throws IOException, NotBoundException {
//        String node_ip=args[0];
//        String server_ip=args[1];
//
//        Node node = new Node(node_ip,server_ip);
//
//        node.start();
//
//        PerformanceEvaluator nodeDriver=new PerformanceEvaluator();
//        nodeDriver.getQueries(node);
//
//        // performanceEvaluate(node);
//    }

    public void perfEval(Node node) {
        getQueries(node);
    }

    public static void performanceEvaluate(Node node) {
        for (int i = 0; i < 5; i++) {
            node.search("ser Tin");
        }
    }

    public void getQueries(Node node) {
        try {
            String line = "";
            String fileName = "Queries.txt";
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(fileName).getFile());

            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(file);
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                System.out.println("Search >> " + line);
                node.search("ser " + line);
            }
            node.writeResults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
