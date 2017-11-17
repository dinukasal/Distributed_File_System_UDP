package Node.Communicators;


import java.io.IOException;
import java.util.List;

import static Node.Constants.SEROK_FORMAT;
import static Node.Constants.SERRM_FORMAT;
import static Node.Constants.SER_FORMAT;


/**
 * Created by nadunindunil on 11/17/17.
 */
public class SearchCommunicatorUDPImpl implements SearchCommunicator {

    @Override
    public void search(String fromIp, int fromPort, String toIp, int toPort, String ownIP, int ownPort, String fileName, int hops) throws IOException {
        String msg = String.format(SER_FORMAT, ownIP, ownPort, fileName, hops);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }

    @Override
    public void searchOK(String fromIp, int fromPort, String toIp, int toPort, String ownIP, int ownPort, List<String>
            searchResult, int no_files, int hops) throws IOException {
        String msg = String.format(SEROK_FORMAT,no_files, fromIp, fromPort, hops, searchResult);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }

    @Override
    public void removeSearchRecords(String fromIp, int fromPort, String toIp, int toPort) throws IOException {
        String msg = String.format(SERRM_FORMAT,fromIp, fromPort);
        String request = Request.create(msg);
        Request.sendSyncMessage(request, toIp, Integer.toString(toPort));
    }

}
