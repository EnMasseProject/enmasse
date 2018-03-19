/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.ArrayList;
import java.util.List;

public class AddressSpace {
    private String name;
    private String namespace;
    private String plan;
    private AddressSpaceType type;
    private AuthService authService;
    private List<AddressSpaceEndpoint> endpoints = new ArrayList<>();

    public AddressSpace(String name) {
        this(name, name, AddressSpaceType.STANDARD, AuthService.NONE);
    }

    public AddressSpace(String name, AuthService authService) {
        this(name, name, AddressSpaceType.STANDARD, authService);
    }

    public AddressSpace(String name, AddressSpaceType type) {
        this(name, name, type, AuthService.NONE);
    }

    public AddressSpace(String name, String namespace) {
        this(name, namespace, AddressSpaceType.STANDARD, AuthService.NONE);
    }

    public AddressSpace(String name, String namespace, AuthService authService) {
        this(name, namespace, AddressSpaceType.STANDARD, authService);
    }

    public AddressSpace(String name, String namespace, String plan) {
        this(name, namespace, AddressSpaceType.STANDARD, plan);
    }

    public AddressSpace(String name, String namespace, String plan, AuthService authService) {
        this(name, namespace, AddressSpaceType.STANDARD, plan, authService);
    }

    public AddressSpace(String name, AddressSpaceType type, String plan) {
        this(name, name, type, plan);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, String plan) {
        this(name, namespace, type, plan, AuthService.NONE);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setAuthService(AuthService.NONE);
    }


    public AddressSpace(String name, AddressSpaceType type, AuthService authService) {
        setName(name);
        setNamespace(name);
        setType(type);
        setAuthService(authService);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, AuthService authService) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setAuthService(authService);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, String plan, AuthService authService) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setPlan(plan);
        setAuthService(authService);
    }

    public AddressSpace(String name, AddressSpaceType type, String plan, AuthService authService) {
        this(name, name, type, plan, authService);
    }


    public AddressSpace setName(String name) {
        this.name = name;
        return this;
    }

    public AddressSpace setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public AddressSpace setPlan(String plan) {
        this.plan = plan;
        return this;
    }

    public AddressSpace setType(AddressSpaceType type) {
        this.type = type;
        if (plan == null) {
            if (type.equals(AddressSpaceType.BROKERED)) {
                plan = "unlimited-brokered";
            } else {
                plan = "unlimited-standard";
            }
        }
        return this;
    }

    public void setEndpoints(List<AddressSpaceEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public List<AddressSpaceEndpoint> getEndpoints() {
        return endpoints;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPlan() {
        return plan;
    }

    public AddressSpaceType getType() {
        return type;
    }

    public AuthService getAuthService() {
        return authService;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("namespace=").append(namespace).append(",")
                .append("type=").append(type.toString().toLowerCase()).append(",")
                .append("plan=").append(plan).append("}")
                .toString();
    }
}
