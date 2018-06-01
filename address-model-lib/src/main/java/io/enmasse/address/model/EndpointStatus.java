/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An endpoint
 */
public class EndpointStatus {
    private final String name;
    private final String host;
    private final int port;
    private final String serviceHost;
    private final Map<String, Integer> servicePorts;

    public EndpointStatus(String name, String serviceHost, String host, int port, Map<String, Integer> servicePorts) {
        this.name = name;
        this.serviceHost = serviceHost;
        this.host = host;
        this.port = port;
        this.servicePorts = servicePorts;
    }

    public String getName() {
        return name;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public int getPort() {
        return port;
    }

    public Map<String, Integer> getServicePorts() {
        return Collections.unmodifiableMap(servicePorts);
    }

    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("host=").append(host).append(",")
                .append("serviceHost=").append(serviceHost).append(",")
                .append("port=").append(port).append("}")
                .toString();
    }

    public static class Builder {
        private String name;
        private String serviceHost;
        private String host;
        private int port = 0;
        private Map<String, Integer> servicePorts = new HashMap<>();

        public Builder() {}

        public Builder(EndpointStatus endpoint) {
            this.name = endpoint.getName();
            this.port = endpoint.getPort();
            this.serviceHost = endpoint.getServiceHost();
            this.host = endpoint.getHost();
            this.servicePorts = new HashMap<>(endpoint.getServicePorts());
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setServiceHost(String serviceHost) {
            this.serviceHost = serviceHost;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }


        public Builder setServicePorts(Map<String, Integer> servicePorts) {
            this.servicePorts = new HashMap<>(servicePorts);
            return this;
        }

        public EndpointStatus build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(serviceHost, "service host not set");
            return new EndpointStatus(name, serviceHost, host, port, servicePorts);
        }
    }
}
