package Node.Communicators;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramSocket;

import static Node.Constants.*;

/**
 * Created by nadunindunil on 11/17/17.
 */
public class NodeCommunicatorUDPImpl implements NodeCommunicator {

    @Override
    public void join(String fromIp, int fromPort, String toIp,int toPort) throws IOException {
        System.out.println("////////////\\\\\\\\\\\\"+fromIp+","+fromPort);
        String msg = String.format(JOIN_FORMAT, fromIp, fromPort);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }

    @Override
    public void joinOK(String fromIp, int fromPort, String toIp, int toPort, DatagramSocket datagramSocket) throws IOException {
        String msg = String.format(JOINOK_FORMAT, 0);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort),datagramSocket);
    }


    @Override
    public void leave(String fromIp, int fromPort, String toIp,int toPort) throws IOException  {
        String msg = String.format(LEAVE_FORMAT, fromIp, fromPort);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }

    @Override
    public void leaveOK(String fromIp, int fromPort, String toIp,int toPort, DatagramSocket datagramSocket ) throws IOException  {
        String msg = String.format(LEAVEOK_FORMAT,0);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort),datagramSocket);
    }

    @Override
    public void sendHeartBeat(String fromIp, int fromPort, String toIp, int toPort) throws IOException {
        String msg = String.format(HB_FORMAT, fromIp, fromPort);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }

    @Override
    public void sendHeartBeatOK(String fromIp, int fromPort, String toIp, int toPort) throws IOException {
        String msg = String.format(HBOK_FORMAT, fromIp, fromPort);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }
}
