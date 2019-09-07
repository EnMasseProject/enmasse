/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Connector {
    private String host;
    private int port;
    private String sslProfile;
    private Boolean verifyHostname;

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

    @Override
    public String toString() {
        return "Connector{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", sslProfile='" + sslProfile + '\'' +
                ", verifyHostname=" + verifyHostname +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connector connector = (Connector) o;
        return port == connector.port &&
                Objects.equals(host, connector.host) &&
                Objects.equals(sslProfile, connector.sslProfile) &&
                Objects.equals(verifyHostname, connector.verifyHostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, sslProfile, verifyHostname);
    }
}
