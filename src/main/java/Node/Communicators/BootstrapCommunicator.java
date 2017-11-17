package Node.Communicators;

import Node.Neighbour;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.List;

/**
 * Created by nadunindunil on 11/16/17.
 */
public interface BootstrapCommunicator {


    List<Neighbour> register(String ipAddress, int port, String username) throws IOException, NotBoundException;

    boolean unregister(String ipAddress, int port, String username) throws IOException;
}
