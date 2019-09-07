/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinkRoute {
    private String name;
    private String prefix;
    private Direction direction;
    private String containerId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public String toString() {
        return "LinkRoute{" +
                "name='" + name + '\'' +
                ", prefix='" + prefix + '\'' +
                ", direction=" + direction +
                ", containerId='" + containerId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkRoute linkRoute = (LinkRoute) o;
        return Objects.equals(name, linkRoute.name) &&
                Objects.equals(prefix, linkRoute.prefix) &&
                direction == linkRoute.direction &&
                Objects.equals(containerId, linkRoute.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, prefix, direction, containerId);
    }

    public enum Direction {
        in,
        out
    }
}
