package Node;

import java.io.IOException;
import java.rmi.NotBoundException;

/**
 * Created by nadunindunil on 11/16/17.
 */
public class NodeDriver {

    public static void main(String[] args) throws IOException, NotBoundException {
        Node node = new Node("127.0.0.1");

        node.start();
    }
}
