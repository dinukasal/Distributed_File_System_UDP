package Node;

import Node.Communicators.*;
import Node.FileSearch.FileSearch;
import Node.FileSearch.FileSearchImpl;
import Node.HeartBeater.HeartBeater;
import Node.HeartBeater.HeartBeaterImpl;

import java.io.IOException;
import java.io.SyncFailedException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nadunindunil on 11/16/17.
 */
public class Node {

    private String ipAddress;
    private int port;
    private String name;
    private String bootstrapServerIp;

    private List<Neighbour> myNeighbours = new ArrayList<>();
    private BootstrapCommunicator bootstrapCommunicator = new BootstrapCommunicatorUDPImpl();
    private NodeCommunicator nodeCommunicator = new NodeCommunicatorUDPImpl();
    private FileSearch fileSearch = new FileSearchImpl(this);
    private NodeServer nodeServer;
    private HeartBeater heartBeater = new HeartBeaterImpl(this,nodeCommunicator);

    private final static Logger LOGGER = Logger.getLogger(Node.class.getName());

    public Node(String ipAddress,String bootstrapServer){
        this.ipAddress = ipAddress;
        this.port = generatePort();
        this.bootstrapServerIp=bootstrapServer;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void addNeighbour(Neighbour node){
        myNeighbours.add(node);
    }


    public void removeNeighbour(String ipAddress, int port) {
        List<Neighbour> remove = new ArrayList<>();
        if (!myNeighbours.isEmpty()) {
            myNeighbours.stream().filter((node) -> (node.getIp().equals(ipAddress) && node.getPort() == port)).forEachOrdered
                    ((node) -> {
                remove.add(node);
            });
        }

        myNeighbours.removeAll(remove);
        LOGGER.log(Level.INFO, "Neighbour size: "+ myNeighbours.size());
    }

    public List<Neighbour> getMyNeighbours() {
        return myNeighbours;
    }

    public void start() throws IOException, NotBoundException {

        LOGGER.log(Level.INFO, "Node server is starting !!!");
        
        System.out.println("IP : " + ipAddress);
        System.out.println("Port : " + port);

        nodeServer = new NodeServerUDPImpl(this, fileSearch,nodeCommunicator,heartBeater);
        fileSearch.start();

        List<Neighbour> nodeList = register();

        nodeServer.start();

        connect(nodeList);
        heartBeater.addNeighboursToHeartBeatList(nodeList);

        heartBeater.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                gracefulDeparture();
                bootstrapCommunicator.unregister(ipAddress, port, name);
                Thread.sleep(2000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    private void gracefulDeparture() throws IOException, NotBoundException {
        for (Neighbour node : myNeighbours) {
            nodeCommunicator.leave(ipAddress,port,node.getIp(),node.getPort());
        }

        System.out.println("Graceful Departure!!!");

        fileSearch.gracefulDeparture();

        myNeighbours.clear();
    }

    private int generatePort() {
        Random r = new Random();
        return Math.abs(r.nextInt()) % 6000 + 3000;
    }

    private List<Neighbour> register() throws IOException, NotBoundException {
        return bootstrapCommunicator.register(ipAddress,port,"n1");
    }

    private void connect(List<Neighbour> nodeList) throws IOException {
        if ( nodeList != null){
            for (Neighbour node: nodeList){
                if ( node.getIp().equals(ipAddress) && node.getPort() != port){
                    // should be node.getIp().equals(ipAddress) in ds
                    nodeCommunicator.join(ipAddress,port,node.getIp(),node.getPort());
                }
            }
        }
    }


}
