package Node.Communicators;

import Node.Constants;
import Node.Neighbour;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nadunindunil on 11/5/17.
 */
public class Request {

    private static byte[] buf = new byte[1000];
    private final static Logger LOGGER = Logger.getLogger(Request.class.getName());
    private static int msgCount=0;

    public static String sendAsyncMessage(String outString, String outAddress, String outPort) throws IOException {

        DatagramSocket datagramSocket = new DatagramSocket();

        DatagramPacket out = new DatagramPacket(outString.getBytes(), outString.getBytes().length,
                InetAddress.getByName(outAddress), Integer.parseInt(outPort));

        LOGGER.log(Level.INFO, "SENDING... => " + outString + " to " + outPort);

        datagramSocket.send(out);
        msgCount++;

        DatagramPacket incoming = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(incoming);

        return new String(incoming.getData(), 0, incoming.getLength());

    }

    public static void sendSyncMessage(String outString, String outAddress, String outPort) throws IOException {

        DatagramSocket datagramSocket = new DatagramSocket();

        DatagramPacket out = new DatagramPacket(outString.getBytes(), outString.getBytes().length,
                InetAddress.getByName(outAddress), Integer.parseInt(outPort));

        LOGGER.log(Level.FINEST, "SENDING... => " + outString + " to " + outPort);
        datagramSocket.send(out);
        msgCount++;

    }

    public static void sendSyncMessage(String outString, String outAddress, String outPort,DatagramSocket datagramSocket) throws IOException {

        DatagramPacket out = new DatagramPacket(outString.getBytes(), outString.getBytes().length,
                InetAddress.getByName(outAddress), Integer.parseInt(outPort));

        LOGGER.log(Level.FINEST, "SENDING... => " + outString + " to " + outPort);
        datagramSocket.send(out);
        msgCount++;

    }

    public static List<Neighbour> decodeRegisterResponse(String response) throws RemoteException, NotBoundException, MalformedURLException {
        StringTokenizer st = new StringTokenizer(response, " ");
        String length = "", command = "";
        length = st.nextToken();
        command = st.nextToken();
        List<Neighbour> nodeList = null;


        if (command.equals(Constants.REGOK)) {

            int no_nodes = Integer.parseInt(st.nextToken());

            switch (no_nodes) {
                case 0:
                    LOGGER.log(Level.INFO, "no nodes in the system");
                    break;
                case 1:
                case 2:
                    nodeList = new ArrayList<>();
                    while (no_nodes > 0) {
                        String param1=st.nextToken();
                        String param2=st.nextToken();
                        nodeList.add(new Neighbour(param1, Integer.parseInt(param2), (float)1.0));
                        no_nodes--;
                    }
                    break;
                case 9996:
                    LOGGER.log(Level.INFO, "failed, can’t register. BS full");
                    break;
                case 9997:
                    LOGGER.log(Level.INFO, "failed, registered to another user, try a different IP and port");
                    break;
                case 9998:
                    LOGGER.log(Level.INFO, "failed, already registered to you, unregister first");
                    break;
                case 9999:
                    LOGGER.log(Level.INFO, "failed, there is some error in the command");
                    break;
                default:
                    LOGGER.log(Level.INFO, "wrong request");

            }
        }

        return nodeList;
    }

    public static boolean decodeUnregister(String response){

        StringTokenizer st = new StringTokenizer(response, " ");
        String status = st.nextToken();

//        if (!Constants.UNROK.equals(status)) {
//            throw new IllegalStateException(Constants.UNROK + " not received");
//        }

        int code = Integer.parseInt(st.nextToken());

        switch (code) {
            case 0:
                LOGGER.log(Level.INFO, "Successful");
                return true;
            case 9999:
                LOGGER.log(Level.INFO, "Error while un-registering. IP and port may not be in the registry or command is incorrect");
            default:
                return false;
        }
    }

    public static String create(String msg){
        return String.format(Constants.MSG_FORMAT, msg.length() + 5, msg);
    }

    public static int getMsgCount(){
        return msgCount;
    }
}
