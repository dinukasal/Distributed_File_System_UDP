package Node.Communicators;

import java.io.IOException;
import java.util.List;

/**
 * Created by nadunindunil on 11/17/17.
 */
public interface SearchCommunicator {

    void search(String fromIp, int fromPort, String toIp,int toPort, String ownIP, int ownPort, String fileName, int
            hops) throws
            IOException;

    void searchOK(String fromIp, int fromPort, String toIp, int toPort, String ownIP, int ownPort, List<String>
            searchResult, int no_files, int hops) throws IOException;

    void removeSearchRecords(String fromIp, int fromPort, String toIp, int toPort) throws IOException;
}
