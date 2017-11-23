package Node.FileSearch;

import java.io.IOException;
import java.util.List;

/**
 * Created by nadunindunil on 11/17/17.
 */
public interface FileSearch {

    void start();

    void searchProcess(int hops, String fileName, String originatorIP, int originatorPort) throws IOException;

    void searchOKProcess(int numberOfHops, List<String> searchResults, String ownerIP, int ownerPort);

    //remove previous search results when graceful departure
    void removeSearchResults(String ip, String port);

    void gracefulDeparture() throws IOException;

    void search(String outMessage);

    void searchWithOk(String outMessage);
}
