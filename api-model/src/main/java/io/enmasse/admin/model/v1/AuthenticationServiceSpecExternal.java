/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.fabric8.kubernetes.api.model.SecretReference;

import java.util.Objects;

public class AuthenticationServiceSpecExternal extends AbstractWithAdditionalProperties {
    private String host;
    private int port;
    private boolean allowOverride = false;
    private SecretReference caCertSecret;
    private SecretReference clientCertSecret;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceSpecExternal that = (AuthenticationServiceSpecExternal) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(allowOverride, that.allowOverride) &&
                Objects.equals(caCertSecret, that.caCertSecret) &&
                Objects.equals(clientCertSecret, that.clientCertSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, allowOverride, caCertSecret, clientCertSecret);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceSpecExternal{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", allowOverride=" + allowOverride +
                ", caCertSecret=" + caCertSecret +
                ", clientCertSecret=" + clientCertSecret +
                '}';
    }

    public boolean isAllowOverride() {
        return allowOverride;
    }

    public void setAllowOverride(boolean allowOverride) {
        this.allowOverride = allowOverride;
    }
}
