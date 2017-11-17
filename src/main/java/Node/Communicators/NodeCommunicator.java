package Node.Communicators;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by nadunindunil on 11/17/17.
 */
public interface NodeCommunicator {

    void join(String fromIp, int fromPort, String toIp,int toPort) throws IOException;

    void joinOK(String fromIp, int fromPort, String toIp, int toPort, DatagramSocket datagramSocket) throws IOException;

    void leave(String fromIp, int fromPort, String toIp, int toPort) throws IOException;

    void leaveOK(String fromIp, int fromPort, String toIp, int toPort, DatagramSocket datagramSocket) throws IOException;

    void sendHeartBeat(String fromIp, int fromPort, String toIp, int toPort) throws IOException;

    void sendHeartBeatOK(String fromIp, int fromPort, String toIp, int toPort) throws IOException;
}
