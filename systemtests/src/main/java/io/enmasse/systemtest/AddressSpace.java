/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class AddressSpace {
    private final String name;
    private final String namespace;
    private String plan;
    private AddressSpaceType type;

    public AddressSpace(String name) {
        this.name = name;
        this.namespace = name;
        this.type = AddressSpaceType.STANDARD;
    }

    public AddressSpace(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
        this.type = AddressSpaceType.STANDARD;
    }

    public AddressSpace(String name, String namespace, String plan) {
        this.name = name;
        this.namespace = namespace;
        this.plan = plan;
        this.type = AddressSpaceType.STANDARD;
    }

    public AddressSpace(String name, AddressSpaceType type) {
        this.name = name;
        this.namespace = name;
        this.type = type;
    }

    public AddressSpace(String name, AddressSpaceType type, String plan) {
        this.name = name;
        this.namespace = name;
        this.type = type;
        this.plan = plan;
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, String plan) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
        this.plan = plan;
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

    public boolean hasPlan() {
        return !(plan == null || plan.isEmpty());
    }

    public AddressSpaceType getType() {
        return type;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("namespace=").append(namespace).append(",")
                .append("type=").append(type.toString().toLowerCase()).append("}")
                .toString();
    }
}
