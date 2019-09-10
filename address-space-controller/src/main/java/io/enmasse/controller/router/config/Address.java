/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address {
    private String name;
    private String prefix;
    private String pattern;
    private Boolean waypoint;
    private Distribution distribution;

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

    public Distribution getDistribution() {
        return distribution;
    }

    public void setDistribution(Distribution distribution) {
        this.distribution = distribution;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Boolean getWaypoint() {
        return waypoint;
    }

    public void setWaypoint(Boolean waypoint) {
        this.waypoint = waypoint;
    }

    @Override
    public String toString() {
        return "Address{" +
                "name='" + name + '\'' +
                ", prefix='" + prefix + '\'' +
                ", pattern='" + pattern + '\'' +
                ", waypoint=" + waypoint +
                ", distribution=" + distribution +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(name, address.name) &&
                Objects.equals(prefix, address.prefix) &&
                Objects.equals(pattern, address.pattern) &&
                Objects.equals(waypoint, address.waypoint) &&
                distribution == address.distribution;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, prefix, pattern, waypoint, distribution);
    }
}
