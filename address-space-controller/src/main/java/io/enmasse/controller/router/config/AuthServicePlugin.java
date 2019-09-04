/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthServicePlugin {
    private String name = "auth_service";
    private String host;
    private int port;
    private String realm;
    private String sslProfile;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getSslProfile() {
        return sslProfile;
    }

    public void setSslProfile(String sslProfile) {
        this.sslProfile = sslProfile;
    }

    @Override
    public String toString() {
        return "AuthServicePlugin{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", realm='" + realm + '\'' +
                ", sslProfile='" + sslProfile + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthServicePlugin that = (AuthServicePlugin) o;
        return port == that.port &&
                Objects.equals(name, that.name) &&
                Objects.equals(host, that.host) &&
                Objects.equals(realm, that.realm) &&
                Objects.equals(sslProfile, that.sslProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, host, port, realm, sslProfile);
    }
}
