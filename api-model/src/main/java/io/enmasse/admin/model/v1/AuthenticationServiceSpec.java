/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
@JsonPropertyOrder({"host", "port", "realm", "caCertSecretName", "clientCertSecretName"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationServiceSpec extends AbstractWithAdditionalProperties {

    @NotNull @Valid
    private String host;
    private int port;

    private String realm;
    private String caCertSecretName;
    private String clientCertSecretName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceSpec that = (AuthenticationServiceSpec) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(realm, that.realm) &&
                Objects.equals(caCertSecretName, that.caCertSecretName) &&
                Objects.equals(clientCertSecretName, that.clientCertSecretName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, realm, caCertSecretName, clientCertSecretName);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceSpec{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", realm='" + realm + '\'' +
                ", caCertSecretName='" + caCertSecretName + '\'' +
                ", clientCertSecretName='" + clientCertSecretName + '\'' +
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getCaCertSecretName() {
        return caCertSecretName;
    }

    public void setCaCertSecretName(String caCertSecretName) {
        this.caCertSecretName = caCertSecretName;
    }

    public String getClientCertSecretName() {
        return clientCertSecretName;
    }

    public void setClientCertSecretName(String clientCertSecretName) {
        this.clientCertSecretName = clientCertSecretName;
    }
}
