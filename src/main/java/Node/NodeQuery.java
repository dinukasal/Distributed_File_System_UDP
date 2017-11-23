package Node;

import com.sun.corba.se.impl.orbutil.graph.NodeData;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.io.*;

/**
 * Created by nadunindunil on 11/16/17.
 */
public class NodeQuery {

    public static void main(String[] args) throws IOException, NotBoundException {
        String node_ip=args[0];
        String server_ip=args[1];

        Node node = new Node(node_ip,server_ip);

        node.start();
        
        NodeQuery nodeDriver=new NodeQuery();
        nodeDriver.getQueries(node);

        // performanceEvaluate(node);
    }

    public static void performanceEvaluate(Node node){
        for(int i=0;i<5;i++){
            node.search("ser Tin");
        }
    }

    public void getQueries(Node node){
        try{
            String line="";
            String fileName="Queries.txt";
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(fileName).getFile());

            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(file);
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                System.out.println("Search >> "+line);
                node.search("ser "+line);            
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
