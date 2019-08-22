/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.fabric8.kubernetes.api.model.SecretReference;

import java.util.Objects;

public class AuthenticationServiceOverrides {
    private String host;
    private Integer port;
    private String realm;
    private SecretReference caCertSecret;
    private SecretReference clientCertSecret;

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

    public SecretReference getCaCertSecret() {
        return caCertSecret;
    }

    public void setCaCertSecret(SecretReference caCertSecret) {
        this.caCertSecret = caCertSecret;
    }

    public SecretReference getClientCertSecret() {
        return clientCertSecret;
    }

    public void setClientCertSecret(SecretReference clientCertSecret) {
        this.clientCertSecret = clientCertSecret;
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
                ", caCertSecret='" + caCertSecret + '\'' +
                ", clientCertSecret='" + realm + '\'' +
                '}';
    }
}
