/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

/**
 * Properties of a Divert
 */
public class DivertInfo {
    private final String name;
    private final String routingName;
    private final String address;
    private final String forwardingAddress;

    public DivertInfo(String name, String routingName, String address, String forwardingAddress) {
        this.name = name;
        this.routingName = routingName;
        this.address = address;
        this.forwardingAddress = forwardingAddress;
    }

    public String getName() {
        return name;
    }

    public String getRoutingName() {
        return routingName;
    }

    public String getAddress() {
        return address;
    }

    public String getForwardingAddress() {
        return forwardingAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DivertInfo that = (DivertInfo) o;

        if (!name.equals(that.name)) return false;
        if (!routingName.equals(that.routingName)) return false;
        if (!address.equals(that.address)) return false;
        return forwardingAddress.equals(that.forwardingAddress);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + routingName.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + forwardingAddress.hashCode();
        return result;
    }
}
