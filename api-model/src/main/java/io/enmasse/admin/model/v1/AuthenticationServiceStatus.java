/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.enmasse.address.model.Phase;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.SecretReference;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {
                @BuildableReference(AbstractWithAdditionalProperties.class)
        },
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"host", "port"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationServiceStatus extends AbstractWithAdditionalProperties {
    private Phase phase;
    private String message;
    private String host;
    private int port;
    private SecretReference caCertSecret;
    private SecretReference clientCertSecret;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceStatus that = (AuthenticationServiceStatus) o;
        return port == that.port &&
                phase == that.phase &&
                Objects.equals(message, that.message) &&
                Objects.equals(host, that.host) &&
                Objects.equals(caCertSecret, that.caCertSecret) &&
                Objects.equals(clientCertSecret, that.clientCertSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phase, message, host, port, caCertSecret, clientCertSecret);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceStatus{" +
                "phase=" + phase +
                ", message='" + message + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", caCertSecret=" + caCertSecret +
                ", clientCertSecret=" + clientCertSecret +
                '}';
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

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
