/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Objects;
import java.util.Optional;

/**
 * An endpoint
 */
public class EndpointSpec {
    private final String name;
    private final String service;
    private final String servicePort;
    private final String host;
    private final CertSpec certSpec;

    public EndpointSpec(String name, String service, String servicePort, String host, CertSpec certSpec) {
        this.name = name;
        this.service = service;
        this.servicePort = servicePort;
        this.host = host;
        this.certSpec = certSpec;
    }

    public String getName() {
        return name;
    }

    public String getService() {
        return service;
    }

    public String getServicePort() {
        return servicePort;
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public Optional<CertSpec> getCertSpec() {
        return Optional.ofNullable(certSpec);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("host=").append(host).append(",")
                .append("service=").append(service).append(",")
                .append("servicePort=").append(servicePort).append(",")
                .append("cert=").append(certSpec).append("}")
                .toString();
    }

    public static class Builder {
        private String name;
        private String service;
        private String servicePort;
        private String host;
        private CertSpec certSpec;

        public Builder() {}

        public Builder(EndpointSpec endpoint) {
            this.name = endpoint.getName();
            this.service = endpoint.getService();
            this.servicePort = endpoint.getServicePort();
            this.host = endpoint.getHost().orElse(null);
            this.certSpec = endpoint.getCertSpec().orElse(null);
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setService(String service) {
            this.service = service;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setServicePort(String servicePort) {
            this.servicePort = servicePort;
            return this;
        }


        public Builder setCertSpec(CertSpec certSpec) {
            this.certSpec = certSpec;
            return this;
        }

        public EndpointSpec build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(service, "service not set");
            Objects.requireNonNull(servicePort, "service port not set");
            return new EndpointSpec(name, service, servicePort, host, certSpec);
        }
    }
}
