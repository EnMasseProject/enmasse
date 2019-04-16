/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Objects;

public class AuthenticationServiceOverrides {
    private String host;
    private Integer port;
    private String realm;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceOverrides that = (AuthenticationServiceOverrides) o;
        return Objects.equals(host, that.host) &&
                Objects.equals(port, that.port) &&
                Objects.equals(realm, that.realm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, realm);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceOverrides{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", realm='" + realm + '\'' +
                '}';
    }
}
