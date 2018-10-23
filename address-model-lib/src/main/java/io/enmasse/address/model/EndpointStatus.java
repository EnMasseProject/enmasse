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
    private final String serviceHost;
    private final Map<String, Integer> servicePorts;
    private final String externalHost;
    private final Map<String, Integer> externalPorts;

    public EndpointStatus(String name, String serviceHost, Map<String, Integer> servicePorts, String externalHost, Map<String, Integer> externalPorts) {
        this.name = name;
        this.serviceHost = serviceHost;
        this.servicePorts = servicePorts;
        this.externalHost = externalHost;
        this.externalPorts = externalPorts;
    }

    public String getName() {
        return name;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public Map<String, Integer> getExternalPorts() {
        return Collections.unmodifiableMap(externalPorts);
    }

    public Map<String, Integer> getServicePorts() {
        return Collections.unmodifiableMap(servicePorts);
    }

    public String getExternalHost() {
        return externalHost;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("externalHost=").append(externalHost).append(",")
                .append("externalPorts=").append(externalPorts).append(",")
                .append("serviceHost=").append(serviceHost).append(",")
                .append("servicePorts=").append(servicePorts).append("}")
                .toString();
    }

    public static class Builder {
        private String name;
        private String serviceHost;
        private Map<String, Integer> servicePorts = new HashMap<>();
        private String externalHost;
        private Map<String, Integer> externalPorts = new HashMap<>();

        public Builder() {}

        public Builder(EndpointStatus endpoint) {
            this.name = endpoint.getName();
            this.serviceHost = endpoint.getServiceHost();
            this.servicePorts = new HashMap<>(endpoint.getServicePorts());
            this.externalHost = endpoint.getExternalHost();
            this.externalPorts = new HashMap<>(endpoint.getExternalPorts());
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setServiceHost(String serviceHost) {
            this.serviceHost = serviceHost;
            return this;
        }

        public Builder setExternalHost(String externalHost) {
            this.externalHost = externalHost;
            return this;
        }

        public Builder setExternalPorts(Map<String, Integer> externalPorts) {
            this.externalPorts = new HashMap<>(externalPorts);
            return this;
        }


        public Builder setServicePorts(Map<String, Integer> servicePorts) {
            this.servicePorts = new HashMap<>(servicePorts);
            return this;
        }

        public EndpointStatus build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(serviceHost, "service host not set");
            return new EndpointStatus(name, serviceHost, servicePorts, externalHost, externalPorts);
        }
    }
}
