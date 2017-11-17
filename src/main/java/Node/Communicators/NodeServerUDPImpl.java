package Node.Communicators;

import Node.FileSearch.FileSearch;
import Node.HeartBeater.HeartBeater;
import Node.Node;
import Node.Neighbour;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static Node.Constants.*;

/**
 * Created by nadunindunil on 11/17/17.
 */
public class NodeServerUDPImpl implements NodeServer {

    private Node node;
    private ExecutorService executorService;
    private FileSearch fileSearch;
    private NodeCommunicator nodeCommunicator;
    private HeartBeater heartBeater;

    private final static Logger LOGGER = Logger.getLogger(NodeServerUDPImpl.class.getName());

    public NodeServerUDPImpl(Node node, FileSearch fileSearch, NodeCommunicator nodeCommunicator,HeartBeater heartBeater) {
        this.node = node;
        this.fileSearch = fileSearch;
        this.nodeCommunicator = nodeCommunicator;
        this.heartBeater = heartBeater;

    }

    @Override
    public void start() {
        executorService = Executors.newCachedThreadPool();
        executorService.submit(() -> {
            try {
                listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void listen() {

        try(DatagramSocket datagramSocket = new DatagramSocket(this.node.getPort())){

            LOGGER.log(Level.INFO, "started Listening........");

            while(true){
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(incoming);

                byte[] data = incoming.getData();
                String request = new String(data, 0, incoming.getLength());

                executorService.submit(() -> {
                    try {
                        handle(request,incoming,datagramSocket);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "exception in NodeServer :", e);
                        e.printStackTrace();
                    }
                });
            }
        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, "exception in NodeServer :", e);
            e.printStackTrace();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "exception in NodeServer :", e);
            e.printStackTrace();
        }

    }

    private void handle(String request, DatagramPacket incoming, DatagramSocket datagramSocket) throws IOException {
        StringTokenizer st = new StringTokenizer(request, " ");
        String command = "", length = "";

        String ip = incoming.getAddress().getHostAddress();
        int incomingPort = incoming.getPort();

        length = st.nextToken();
        command = st.nextToken();

        LOGGER.log(Level.FINEST, "RECEIVED <= " + request + " from " + ip + "," + incomingPort);

        String ip_address;
        int port_num;
        String fileName;
        int hops;
        List<String> fileNames = new ArrayList<>();

        switch (command){
            case JOIN:
                ip_address = st.nextToken();
                port_num = Integer.parseInt(st.nextToken());
                try {
                    joinHandler(ip_address,port_num,datagramSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case JOINOK:
                LOGGER.log(Level.INFO, "join OK");
                joinOKHandler(ip,incomingPort,Integer.parseInt(st.nextToken()));
                break;
            case SER:
                ip_address = st.nextToken();
                port_num = Integer.parseInt(st.nextToken());
                fileName = st.nextToken();
                hops = Integer.parseInt(st.nextToken());
                searchHandler(ip_address,port_num,fileName,hops);
                break;
            case SEROK:
                LOGGER.log(Level.INFO, "SER OK");

                String no_files = st.nextToken();
                ip_address = st.nextToken();
                port_num = Integer.parseInt(st.nextToken());
                hops = Integer.parseInt(st.nextToken());

                while (st.hasMoreTokens()){
                    fileNames.add(st.nextToken());
                }

                searchOKHandler(ip_address,port_num,fileNames,hops);
                break;
            case SERRM:
                ip_address = st.nextToken();
                String sPort = st.nextToken();
                searchRemoveHandler(ip_address,sPort);
                break;
            case LEAVE:
                ip_address = st.nextToken();
                port_num = Integer.parseInt(st.nextToken());
                leaveHandler(ip_address,port_num,datagramSocket);
                break;
            case LEAVEOK:
                leaveOKHandler();
                break;
            case HB:
                ip_address = st.nextToken();
                port_num = Integer.parseInt(st.nextToken());
                heartBeatHandler(ip_address,port_num);
                break;
            case HBOK:
                ip_address = st.nextToken();
                port_num = Integer.parseInt(st.nextToken());
                heartBeatOKHandler(ip_address,port_num);
                break;
            default:
                LOGGER.log(Level.INFO, "not a valid request");
                break;
        }
    }

    private void joinOKHandler(String ip, int port,int code){

        switch (code) {
            case 0:
                LOGGER.log(Level.INFO, "Successful Join");
                this.node.addNeighbour(new Neighbour(ip,port,(float)1.0));
                break;
            case 9999:
                LOGGER.log(Level.INFO, "error while adding new node to routing table");
            default:
                break;
        }
    }

    private void joinHandler(String ip, int port, DatagramSocket datagramSocket) throws IOException {

        node.addNeighbour(new Neighbour(ip,port,(float)1.0));
        heartBeater.addNeighboursToHeartBeatList(node.getMyNeighbours());

        LOGGER.log(Level.INFO, "node added in handler");

        nodeCommunicator.joinOK(node.getIpAddress(),node.getPort(),ip,port,datagramSocket);

    }

    private void searchHandler(String ipAddress, int port ,String fileName, int hops) throws IOException {

        fileSearch.searchProcess(hops,fileName,ipAddress,port);

    }

    private void searchOKHandler(String ipAddress, int port ,List<String> fileNames, int hops) {

        fileSearch.searchOKProcess(hops,fileNames,ipAddress,port);

    }

    private void searchRemoveHandler(String ipAddress, String port){
        fileSearch.removeSearchResults(ipAddress,port);
    }

    private void leaveHandler(String ip, int port, DatagramSocket datagramSocket) throws IOException {

        node.removeNeighbour(ip,port);

        LOGGER.log(Level.INFO, "node removed in handler");

        nodeCommunicator.leaveOK(node.getIpAddress(),node.getPort(),ip,port,datagramSocket);
    }

    private void leaveOKHandler(){
        System.out.println("inside leaveOK");
        // TODO
    }

    private void heartBeatHandler(String ip, int port) throws IOException {
        nodeCommunicator.sendHeartBeatOK(node.getIpAddress(),node.getPort(),ip,port);
    }

    private void heartBeatOKHandler(String ip, int port){
        heartBeater.processHeartBeatOK(ip,port);
    }

}
