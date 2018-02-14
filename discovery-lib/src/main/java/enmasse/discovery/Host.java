/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.discovery;

import java.util.Map;

/**
 * Represents a host with a name and a set of ports.
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

    public Endpoint amqpEndpoint() {
        return getEndpoint("amqp");
    }

    public Endpoint getEndpoint(String portName) {
        return new Endpoint(hostname, portMap.get(portName));
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
        if (portMap.containsKey("amqp")) {
            result = 31 * result + portMap.get("amqp");
        }
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
