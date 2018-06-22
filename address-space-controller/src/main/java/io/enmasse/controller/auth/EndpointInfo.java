/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.CertSpec;

import java.util.*;

class EndpointInfo {
    private final String serviceName;
    private final CertSpec certs;
    private final List<String> hosts = new ArrayList<>();

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

    public List<String> getHosts() {
        return hosts;
    }

    public EndpointInfo addHost(String host) {
        hosts.add(host);
        return this;
    }
}
