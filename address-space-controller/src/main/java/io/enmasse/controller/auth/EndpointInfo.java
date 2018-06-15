/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.CertSpec;

import java.util.HashSet;
import java.util.Set;

class EndpointInfo {
    private final String serviceName;
    private final CertSpec certs;
    private final Set<String> hosts = new HashSet<>();

    EndpointInfo(String serviceName, CertSpec certs) {
        this.serviceName = serviceName;
        this.certs = certs;
    }

    public String getServiceName() {
        return serviceName;
    }

    public CertSpec getCertSpec() {
        return certs;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public EndpointInfo addHost(String host) {
        hosts.add(host);
        return this;
    }
}
