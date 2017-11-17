package Node;

/**
 * Created by nadunindunil on 11/16/17.
 */
public class Neighbour {
    private String ip;
    private int port;
    private String username;
    private float probability;

    public Neighbour(String ip, int port, float probability){
        this.ip = ip;
        this.port = port;
        this.probability = probability;
    }

    public Neighbour(String ip, int port, String username, float probability){
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.probability = probability;
    }

    public String getIp(){
        return this.ip;
    }

    public String getUsername(){
        return this.username;
    }

    public int getPort(){
        return this.port;
    }

    public float getProbability() {
        return probability;
    }
}
