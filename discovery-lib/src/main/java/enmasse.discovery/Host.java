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

    public Map<String, Integer> getPortMap() {
        return portMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        if (!hostname.equals(host.hostname)) return false;
        return portMap.equals(host.portMap);

    }

    @Override
    public int hashCode() {
        int result = hostname.hashCode();
        result = 31 * result + portMap.hashCode();
        return result;
    }
}
