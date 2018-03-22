/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class AddressSpaceEndpoint {
    private String name;
    private String service;
    private String host;

    public AddressSpaceEndpoint(String name, String service) {
        this.name = name;
        this.service = service;
    }

    public AddressSpaceEndpoint(String name, String service, String host) {
        this(name, service);
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}
