/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
        )
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceSpecConnectorTls extends AbstractWithAdditionalProperties {
    private StringOrSecretSelector caCert;
    private StringOrSecretSelector clientCert;
    private StringOrSecretSelector clientKey;

    public StringOrSecretSelector getCaCert() {
        return caCert;
    }

    public void setCaCert(StringOrSecretSelector caCert) {
        this.caCert = caCert;
    }

    public StringOrSecretSelector getClientCert() {
        return clientCert;
    }

    public void setClientCert(StringOrSecretSelector clientCert) {
        this.clientCert = clientCert;
    }

    public StringOrSecretSelector getClientKey() {
        return clientKey;
    }

    public void setClientKey(StringOrSecretSelector clientKey) {
        this.clientKey = clientKey;
    }

    @Override
    public String toString() {
        return "AddressSpaceSpecConnectorTls{" +
                "caCert=" + caCert +
                ", clientCert=" + clientCert +
                ", clientKey=" + clientKey +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSpecConnectorTls that = (AddressSpaceSpecConnectorTls) o;
        return Objects.equals(caCert, that.caCert) &&
                Objects.equals(clientCert, that.clientCert) &&
                Objects.equals(clientKey, that.clientKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caCert, clientCert, clientKey);
    }
}
