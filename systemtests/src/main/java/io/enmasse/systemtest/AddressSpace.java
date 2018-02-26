/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

public class AddressSpace {
    private String name;
    private String namespace;
    private String plan;
    private AddressSpaceType type;

    public AddressSpace(String name) {
        this(name, name, AddressSpaceType.STANDARD);
    }

    public AddressSpace(String name, String namespace) {
        this(name, namespace, AddressSpaceType.STANDARD);
    }

    public AddressSpace(String name, String namespace, String plan) {
        this(name, name, AddressSpaceType.STANDARD, plan);
    }

    public AddressSpace(String name, AddressSpaceType type) {
        this(name, name, type);
    }

    public AddressSpace(String name, AddressSpaceType type, String plan) {
        this(name, name, type, plan);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type) {
        setName(name);
        setNamespace(namespace);
        setType(type);
    }

    public AddressSpace(String name, String namespace, AddressSpaceType type, String plan) {
        setName(name);
        setNamespace(namespace);
        setType(type);
        setPlan(plan);
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
