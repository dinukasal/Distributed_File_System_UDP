package Node;

import java.io.IOException;
import java.rmi.NotBoundException;

/**
 * Created by nadunindunil on 11/16/17.
 */
public class NodeDriver {

    public static void main(String[] args) throws IOException, NotBoundException {
        String node_ip=args[0];
        String server_ip=args[1];

        Node node = new Node(node_ip,server_ip);

        node.start();
    }
}
