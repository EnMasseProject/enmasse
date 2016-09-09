package enmasse.broker.prestop;

/**
 * @author Ulf Lilleengen
 */
public class Endpoint {
    private final String hostName;
    private final int port;

    public Endpoint(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public String hostName() {
        return hostName;
    }

    public int port() {
        return port;
    }
}
