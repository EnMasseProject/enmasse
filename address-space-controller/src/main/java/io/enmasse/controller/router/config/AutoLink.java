/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoLink {
    private String name;
    private String address;
    private LinkDirection direction;
    private String containerId;
    private String connection;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LinkDirection getDirection() {
        return direction;
    }

    public void setDirection(LinkDirection direction) {
        this.direction = direction;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "AutoLink{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", direction=" + direction +
                ", containerId='" + containerId + '\'' +
                ", connection='" + connection + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoLink autoLink = (AutoLink) o;
        return Objects.equals(name, autoLink.name) &&
                Objects.equals(address, autoLink.address) &&
                direction == autoLink.direction &&
                Objects.equals(containerId, autoLink.containerId) &&
                Objects.equals(connection, autoLink.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, direction, containerId, connection);
    }
}
