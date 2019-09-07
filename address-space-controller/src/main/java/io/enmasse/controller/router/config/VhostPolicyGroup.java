/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VhostPolicyGroup {
    private String remoteHosts;
    private String sources;
    private String targets;
    private Integer maxFrameSize;
    private Integer maxSessionWindow;
    private Integer maxSessions;
    private Integer maxSenders;
    private Integer maxReceivers;
    private Boolean allowDynamicSource;
    private Boolean allowAnonymousSender;

    public String getRemoteHosts() {
        return remoteHosts;
    }

    public void setRemoteHosts(String remoteHosts) {
        this.remoteHosts = remoteHosts;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public String getTargets() {
        return targets;
    }

    public void setTargets(String targets) {
        this.targets = targets;
    }

    public Integer getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(Integer maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public Integer getMaxSessionWindow() {
        return maxSessionWindow;
    }

    public void setMaxSessionWindow(Integer maxSessionWindow) {
        this.maxSessionWindow = maxSessionWindow;
    }

    public Integer getMaxSessions() {
        return maxSessions;
    }

    public void setMaxSessions(Integer maxSessions) {
        this.maxSessions = maxSessions;
    }

    public Integer getMaxSenders() {
        return maxSenders;
    }

    public void setMaxSenders(Integer maxSenders) {
        this.maxSenders = maxSenders;
    }

    public Integer getMaxReceivers() {
        return maxReceivers;
    }

    public void setMaxReceivers(Integer maxReceivers) {
        this.maxReceivers = maxReceivers;
    }

    public Boolean getAllowDynamicSource() {
        return allowDynamicSource;
    }

    public void setAllowDynamicSource(Boolean allowDynamicSource) {
        this.allowDynamicSource = allowDynamicSource;
    }

    public Boolean getAllowAnonymousSender() {
        return allowAnonymousSender;
    }

    public void setAllowAnonymousSender(Boolean allowAnonymousSender) {
        this.allowAnonymousSender = allowAnonymousSender;
    }

    @Override
    public String toString() {
        return "VhostPolicyGroup{" +
                "remoteHosts='" + remoteHosts + '\'' +
                ", sources='" + sources + '\'' +
                ", targets='" + targets + '\'' +
                ", maxFrameSize=" + maxFrameSize +
                ", maxSessionWindow=" + maxSessionWindow +
                ", maxSessions=" + maxSessions +
                ", maxSenders=" + maxSenders +
                ", maxReceivers=" + maxReceivers +
                ", allowDynamicSource=" + allowDynamicSource +
                ", allowAnonymousSender=" + allowAnonymousSender +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VhostPolicyGroup that = (VhostPolicyGroup) o;
        return Objects.equals(remoteHosts, that.remoteHosts) &&
                Objects.equals(sources, that.sources) &&
                Objects.equals(targets, that.targets) &&
                Objects.equals(maxFrameSize, that.maxFrameSize) &&
                Objects.equals(maxSessionWindow, that.maxSessionWindow) &&
                Objects.equals(maxSessions, that.maxSessions) &&
                Objects.equals(maxSenders, that.maxSenders) &&
                Objects.equals(maxReceivers, that.maxReceivers) &&
                Objects.equals(allowDynamicSource, that.allowDynamicSource) &&
                Objects.equals(allowAnonymousSender, that.allowAnonymousSender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHosts, sources, targets, maxFrameSize, maxSessionWindow, maxSessions, maxSenders, maxReceivers, allowDynamicSource, allowAnonymousSender);
    }
}
