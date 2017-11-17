package Node.HeartBeater;

import Node.Neighbour;

import java.util.List;

/**
 * Created by nadunindunil on 11/17/17.
 */
public interface HeartBeater {
    void start();

    void addNeighboursToHeartBeatList(List<Neighbour> nodeList);

    void processHeartBeatOK(String ipAddress, int port);
}
