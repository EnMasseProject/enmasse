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
    private final ExposeSpec exposeSpec;
    private final CertSpec certSpec;

    public EndpointSpec(String name, String service, ExposeSpec exposeSpec, CertSpec certSpec) {
        this.name = name;
        this.service = service;
        this.exposeSpec = exposeSpec;
        this.certSpec = certSpec;
    }

    public String getName() {
        return name;
    }

    public String getService() {
        return service;
    }

    public Optional<ExposeSpec> getExposeSpec() {
        return Optional.ofNullable(exposeSpec);
    }

    public Optional<CertSpec> getCertSpec() {
        return Optional.ofNullable(certSpec);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("expose=").append(exposeSpec).append(",")
                .append("service=").append(service).append(",")
                .append("cert=").append(certSpec).append("}")
                .toString();
    }

    public void validate() {
        if (certSpec != null) {
            certSpec.validate();
        }
    }

    public static class Builder {
        private String name;
        private String service;
        private ExposeSpec exposeSpec;
        private CertSpec certSpec;

        public Builder() {}

        public Builder(EndpointSpec endpoint) {
            this.name = endpoint.getName();
            this.service = endpoint.getService();
            this.exposeSpec = endpoint.getExposeSpec().orElse(null);
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

        public Builder setExposeSpec(ExposeSpec exposeSpec) {
            this.exposeSpec = exposeSpec;
            return this;
        }

        public Builder setCertSpec(CertSpec certSpec) {
            this.certSpec = certSpec;
            return this;
        }

        public EndpointSpec build() {
            Objects.requireNonNull(name, "name not set");
            Objects.requireNonNull(service, "service not set");
            return new EndpointSpec(name, service, exposeSpec, certSpec);
        }
    }
}
