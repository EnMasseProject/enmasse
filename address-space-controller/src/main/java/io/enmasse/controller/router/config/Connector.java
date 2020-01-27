/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Connector {
    private String name;
    private String host;
    private int port;
    private String sslProfile;
    private Boolean verifyHostname;
    private String saslUsername;
    private String saslPassword;
    private String saslMechanisms;
    private String failoverUrls;
    private Role role;

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

    public String getSslProfile() {
        return sslProfile;
    }

    public void setSslProfile(String sslProfile) {
        this.sslProfile = sslProfile;
    }

    public Boolean getVerifyHostname() {
        return verifyHostname;
    }

    public void setVerifyHostname(Boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
    }

    public String getSaslUsername() {
        return saslUsername;
    }

    public void setSaslUsername(String saslUsername) {
        this.saslUsername = saslUsername;
    }

    public String getSaslPassword() {
        return saslPassword;
    }

    public void setSaslPassword(String saslPassword) {
        this.saslPassword = saslPassword;
    }

    public String getSaslMechanisms() {
        return saslMechanisms;
    }

    public void setSaslMechanisms(String saslMechanisms) {
        this.saslMechanisms = saslMechanisms;
    }

    public String getFailoverUrls() {
        return failoverUrls;
    }

    public void setFailoverUrls(String failoverUrls) {
        this.failoverUrls = failoverUrls;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Connector{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", sslProfile='" + sslProfile + '\'' +
                ", verifyHostname=" + verifyHostname +
                ", saslMechanisms='" + saslMechanisms + '\'' +
                ", failoverUrls='" + failoverUrls + '\'' +
                ", role=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connector connector = (Connector) o;
        return port == connector.port &&
                Objects.equals(name, connector.name) &&
                Objects.equals(host, connector.host) &&
                Objects.equals(sslProfile, connector.sslProfile) &&
                Objects.equals(verifyHostname, connector.verifyHostname) &&
                Objects.equals(saslUsername, connector.saslUsername) &&
                Objects.equals(saslPassword, connector.saslPassword) &&
                Objects.equals(saslMechanisms, connector.saslMechanisms) &&
                Objects.equals(failoverUrls, connector.failoverUrls) &&
                role == connector.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, host, port, sslProfile, verifyHostname, saslUsername, saslPassword, saslMechanisms, failoverUrls, role);
    }
}
