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
    private final CertProvider certProvider;

    public Endpoint(String name, String service, String host, int port, CertProvider certProvider) {
        this.name = name;
        this.service = service;
        this.host = host;
        this.port = port;
        this.certProvider = certProvider;
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

    public Optional<CertProvider> getCertProvider() {
        return Optional.ofNullable(certProvider);
    }

    public static class Builder {
        private String name;
        private String service;
        private String host;
        private int port = 0;
        private CertProvider certProvider;

        public Builder() {}

        public Builder(io.enmasse.address.model.Endpoint endpoint) {
            this.name = endpoint.getName();
            this.service = endpoint.getService();
            this.host = endpoint.getHost().orElse(null);
            this.certProvider = endpoint.getCertProvider().orElse(null);
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

        public Builder setCertProvider(CertProvider certProvider) {
            this.certProvider = certProvider;
            return this;
        }

        public Endpoint build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(service, "service not set");
            return new Endpoint(name, service, host, port, certProvider);
        }
    }
}
