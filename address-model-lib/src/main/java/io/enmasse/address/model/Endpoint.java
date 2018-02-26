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
public class Endpoint {
    private final String name;
    private final String service;
    private final String host;
    private final int port;
    private final CertProviderSpec certProviderSpec;

    public Endpoint(String name, String service, String host, int port, CertProviderSpec certProviderSpec) {
        this.name = name;
        this.service = service;
        this.host = host;
        this.port = port;
        this.certProviderSpec = certProviderSpec;
    }

    public String getName() {
        return name;
    }

    public String getService() {
        return service;
    }

    public int getPort() {
        return port;
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public Optional<CertProviderSpec> getCertProviderSpec() {
        return Optional.ofNullable(certProviderSpec);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("host=").append(host).append(",")
                .append("service=").append(service).append(",")
                .append("port=").append(port).append(",")
                .append("certProviderSpec").append(certProviderSpec).append("}")
                .toString();
    }

    public static class Builder {
        private String name;
        private String service;
        private String host;
        private int port = 0;
        private CertProviderSpec certProviderSpec;

        public Builder() {}

        public Builder(io.enmasse.address.model.Endpoint endpoint) {
            this.name = endpoint.getName();
            this.port = endpoint.getPort();
            this.service = endpoint.getService();
            this.host = endpoint.getHost().orElse(null);
            this.certProviderSpec = endpoint.getCertProviderSpec().orElse(null);
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

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setCertProviderSpec(CertProviderSpec certProviderSpec) {
            this.certProviderSpec = certProviderSpec;
            return this;
        }

        public Endpoint build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(service, "service not set");
            return new Endpoint(name, service, host, port, certProviderSpec);
        }
    }
}
