package enmasse.discovery;

import java.util.Map;

/**
 * @author Ulf Lilleengen
 */
public class Host {
    private final String hostname;
    private final Map<String, Integer> portMap;

    public Host(String hostname, Map<String, Integer> portMap) {
        this.hostname = hostname;
        this.portMap = portMap;
    }

    public String getHostname() {
        return hostname;
    }

    public int getAmqpPort() {
        return portMap.get("amqp");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        if (!hostname.equals(host.hostname)) return false;
        return (portMap.containsKey("amqp") == host.portMap.containsKey("amqp")) && portMap.get("amqp").equals(host.portMap.get("amqp"));

    }

    @Override
    public int hashCode() {
        int result = hostname.hashCode();
        result = 31 * result + portMap.get("amqp");
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("host=").append(hostname).append(", ");
        builder.append("ports=").append(portMap.toString());
        return builder.toString();
    }
}
