package Node.HeartBeater;

import Node.Communicators.NodeCommunicator;
import Node.Neighbour;
import Node.Node;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nadunindunil on 11/17/17.
 */
public class HeartBeaterImpl implements HeartBeater {


    private volatile HashMap<Neighbour, Integer> MyNeighbourHeartBeats = new HashMap<>();
    private final Object lock = new Object();
    private Node node;
    private NodeCommunicator nodeCommunicator;

    private final static Logger LOGGER = Logger.getLogger(HeartBeaterImpl.class.getName());

    public HeartBeaterImpl(Node node, NodeCommunicator nodeCommunicator) {
        this.node = node;
        this.nodeCommunicator = nodeCommunicator;
    }

    @Override
    public void start(){

        Runnable runnableHeartBeatSender = () -> {
            Timer timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        processHeartBeatSend();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 1 * 1000, 1 * 1000);
        };
        Thread heartBeatSenderThread = new Thread(runnableHeartBeatSender);
        heartBeatSenderThread.start();

        Runnable runnableHeartBeatReceiver = () -> {
            Timer timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        processHeartBeatReceive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 3 * 1000, 3 * 1000);
        };
        Thread heartBeatReceiveThread = new Thread(runnableHeartBeatReceiver);
        heartBeatReceiveThread.start();

    }

    @Override
    public void addNeighboursToHeartBeatList(List<Neighbour> nodeList) {
        synchronized (lock) {
            if(nodeList != null) {
                nodeList.stream().forEachOrdered((node) -> MyNeighbourHeartBeats.put(node, 0));
            }

        }
        LOGGER.getLogger(HeartBeaterImpl.class.getName()).log(Level.INFO,"Size....." + MyNeighbourHeartBeats.size());
    }

    @Override
    public void processHeartBeatOK(String ipAddress, int port) {
        synchronized (lock) {
            MyNeighbourHeartBeats.forEach((index, value) -> {
//                System.out.println(index.getPort() + "," + port);
//                System.out.println(index.getIp() + "," + ipAddress);
                if (ipAddress.equals(index.getIp()) && index.getPort() == port) {
                    MyNeighbourHeartBeats.put(index, 0);
                }
            });
        }
    }

    private void processHeartBeatSend() throws IOException {

        List<Neighbour> MyNeighbours = node.getMyNeighbours();
        if (!MyNeighbours.isEmpty()) {
            for (Neighbour iterate_node : MyNeighbours) {
                if (node.getPort() != iterate_node.getPort()) {
//                    System.out.println("details :" + iterate_node.getIp() + "," + iterate_node.getPort());
                    nodeCommunicator.sendHeartBeat(node.getIpAddress(),node.getPort(),iterate_node.getIp(),
                            iterate_node.getPort());
                }
            }
        }
    }

    private void processHeartBeatReceive() throws InterruptedException {

        synchronized (lock) {
            if (MyNeighbourHeartBeats != null && MyNeighbourHeartBeats.size() > 0) {
                MyNeighbourHeartBeats.forEach((index, value) -> {
//                    System.out.println(index + ", " + value);
                    if (value < -4) {
                        try {
                            // heartbeat has been lost for three times!!!
                            node.removeNeighbour(index.getIp(), index.getPort());
                            removeHeartBeater(index.getIp(), index.getPort());
                            System.out.println("removing node :" + index.getPort() + " from system");
                        } catch (InterruptedException ex) {
                            LOGGER.getLogger(HeartBeaterImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
//                        System.out.println("decreasing 1 from node, value: " + value);
                        MyNeighbourHeartBeats.put(index, MyNeighbourHeartBeats.get(index) - 1);
                    }
                });
            } else {
                // hbt neigbour list 0 action   -- empty neighbour list
                try {
                    //bootstrapCommunicator.unregister(this.ipAddress, this.nodePort, this.name);
                    //start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void removeHeartBeater(String ipAddress, int port) throws InterruptedException {

        synchronized (lock) {

            Neighbour neighbour = null;
            for (Neighbour key : MyNeighbourHeartBeats.keySet()) {
                if (key.getIp().equals(ipAddress) && key.getPort() == port) {
                    neighbour = key;
                }
            }
            MyNeighbourHeartBeats.remove(neighbour);
            if (MyNeighbourHeartBeats.size() == 0) {
                System.out.println("empty neighour list");
            }
        }
    }
}
