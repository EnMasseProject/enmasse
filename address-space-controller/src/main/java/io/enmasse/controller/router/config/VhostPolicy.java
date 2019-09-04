/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VhostPolicy {
    private String hostname;
    private Boolean allowUnknownUser;
    private Integer maxConnections;
    private Integer maxConnectionsPerUser;
    private Integer maxConnectionsPerHost;
    private Map<String, VhostPolicyGroup> groups;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Boolean getAllowUnknownUser() {
        return allowUnknownUser;
    }

    public void setAllowUnknownUser(Boolean allowUnknownUser) {
        this.allowUnknownUser = allowUnknownUser;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    public void setMaxConnectionsPerUser(Integer maxConnectionsPerUser) {
        this.maxConnectionsPerUser = maxConnectionsPerUser;
    }

    public Integer getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(Integer maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public Map<String, VhostPolicyGroup> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, VhostPolicyGroup> groups) {
        this.groups = groups;
    }

    @Override
    public String toString() {
        return "VhostPolicy{" +
                "hostname='" + hostname + '\'' +
                ", allowUnknownUser=" + allowUnknownUser +
                ", maxConnections=" + maxConnections +
                ", maxConnectionsPerUser=" + maxConnectionsPerUser +
                ", maxConnectionsPerHost=" + maxConnectionsPerHost +
                ", groups=" + groups +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VhostPolicy that = (VhostPolicy) o;
        return Objects.equals(hostname, that.hostname) &&
                Objects.equals(allowUnknownUser, that.allowUnknownUser) &&
                Objects.equals(maxConnections, that.maxConnections) &&
                Objects.equals(maxConnectionsPerUser, that.maxConnectionsPerUser) &&
                Objects.equals(maxConnectionsPerHost, that.maxConnectionsPerHost) &&
                Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, allowUnknownUser, maxConnections, maxConnectionsPerUser, maxConnectionsPerHost, groups);
    }
}
