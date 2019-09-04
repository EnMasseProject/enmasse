/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Listener {
    private String host;
    private int port;
    private Role role;
    private Boolean authenticatePeer;
    private Boolean http;
    private Boolean requireSsl;
    private Boolean metrics;
    private Boolean healthz;
    private Boolean websockets;
    private String httpRootDir;
    private String saslPlugin;
    private String sslProfile;
    private String saslMechanisms;
    private Integer idleTimeoutSeconds;
    private Integer linkCapacity;
    private Integer initialHandshakeTimeoutSeconds;
    private String policyVhost;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Boolean getAuthenticatePeer() {
        return authenticatePeer;
    }

    public void setAuthenticatePeer(Boolean authenticatePeer) {
        this.authenticatePeer = authenticatePeer;
    }

    public Boolean getHttp() {
        return http;
    }

    public void setHttp(Boolean http) {
        this.http = http;
    }

    public Boolean getMetrics() {
        return metrics;
    }

    public void setMetrics(Boolean metrics) {
        this.metrics = metrics;
    }

    public Boolean getWebsockets() {
        return websockets;
    }

    public void setWebsockets(Boolean websockets) {
        this.websockets = websockets;
    }

    public String getHttpRootDir() {
        return httpRootDir;
    }

    public void setHttpRootDir(String httpRootDir) {
        this.httpRootDir = httpRootDir;
    }

    public String getSaslPlugin() {
        return saslPlugin;
    }

    public void setSaslPlugin(String saslPlugin) {
        this.saslPlugin = saslPlugin;
    }

    public Integer getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(Integer idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public Integer getLinkCapacity() {
        return linkCapacity;
    }

    public void setLinkCapacity(Integer linkCapacity) {
        this.linkCapacity = linkCapacity;
    }

    public Integer getInitialHandshakeTimeoutSeconds() {
        return initialHandshakeTimeoutSeconds;
    }

    public void setInitialHandshakeTimeoutSeconds(Integer initialHandshakeTimeoutSeconds) {
        this.initialHandshakeTimeoutSeconds = initialHandshakeTimeoutSeconds;
    }

    public String getPolicyVhost() {
        return policyVhost;
    }

    public void setPolicyVhost(String policyVhost) {
        this.policyVhost = policyVhost;
    }

    public Boolean getHealthz() {
        return healthz;
    }

    public void setHealthz(Boolean healthz) {
        this.healthz = healthz;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getSslProfile() {
        return sslProfile;
    }

    public void setSslProfile(String sslProfile) {
        this.sslProfile = sslProfile;
    }

    public String getSaslMechanisms() {
        return saslMechanisms;
    }

    public void setSaslMechanisms(String saslMechanisms) {
        this.saslMechanisms = saslMechanisms;
    }

    public Boolean getRequireSsl() {
        return requireSsl;
    }

    public void setRequireSsl(Boolean requireSsl) {
        this.requireSsl = requireSsl;
    }

    @Override
    public String toString() {
        return "Listener{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", role=" + role +
                ", authenticatePeer=" + authenticatePeer +
                ", http=" + http +
                ", requireSsl=" + requireSsl +
                ", metrics=" + metrics +
                ", sslProfile=" + sslProfile +
                ", saslMechanisms=" + saslMechanisms +
                ", healthz=" + metrics +
                ", websockets=" + websockets +
                ", httpRootDir='" + httpRootDir + '\'' +
                ", saslPlugin='" + saslPlugin + '\'' +
                ", idleTimeoutSeconds=" + idleTimeoutSeconds +
                ", linkCapacity=" + linkCapacity +
                ", initialHandshakeTimeoutSeconds=" + initialHandshakeTimeoutSeconds +
                ", policyVhost='" + policyVhost + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Listener listener = (Listener) o;
        return port == listener.port &&
                Objects.equals(host, listener.host) &&
                Objects.equals(role, listener.role) &&
                Objects.equals(authenticatePeer, listener.authenticatePeer) &&
                Objects.equals(http, listener.http) &&
                Objects.equals(requireSsl, listener.requireSsl) &&
                Objects.equals(metrics, listener.metrics) &&
                Objects.equals(sslProfile, listener.sslProfile) &&
                Objects.equals(saslMechanisms, listener.saslMechanisms) &&
                Objects.equals(healthz, listener.healthz) &&
                Objects.equals(websockets, listener.websockets) &&
                Objects.equals(httpRootDir, listener.httpRootDir) &&
                Objects.equals(saslPlugin, listener.saslPlugin) &&
                Objects.equals(idleTimeoutSeconds, listener.idleTimeoutSeconds) &&
                Objects.equals(linkCapacity, listener.linkCapacity) &&
                Objects.equals(initialHandshakeTimeoutSeconds, listener.initialHandshakeTimeoutSeconds) &&
                Objects.equals(policyVhost, listener.policyVhost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, role, authenticatePeer, http, requireSsl, metrics, sslProfile, saslMechanisms, healthz, websockets, httpRootDir, saslPlugin, idleTimeoutSeconds, linkCapacity, initialHandshakeTimeoutSeconds, policyVhost);
    }
}
