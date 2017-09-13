/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
