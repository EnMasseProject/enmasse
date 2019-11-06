/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.enmasse.model.validation.ValidBase64;
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
public class CertSpec extends AbstractWithAdditionalProperties {
    private String provider;
    private String secretName;
    @ValidBase64
    private String tlsKey;
    @ValidBase64
    private String tlsCert;

    public CertSpec() {
    }

    public CertSpec(String provider, String secretName, String tlsKey, String tlsCert) {
        this.provider = provider;
        this.secretName = secretName;
        this.tlsKey = tlsKey;
        this.tlsCert = tlsCert;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

    public String getSecretName() {
        return secretName;
    }

    public void setTlsKey(String tlsKey) {
        this.tlsKey = tlsKey;
    }

    public String getTlsKey() {
        return tlsKey;
    }

    public void setTlsCert(String tlsCert) {
        this.tlsCert = tlsCert;
    }

    public String getTlsCert() {
        return tlsCert;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CertSpec certSpec = (CertSpec) o;
        return Objects.equals(provider, certSpec.provider) &&
                Objects.equals(secretName, certSpec.secretName) &&
                Objects.equals(tlsKey, certSpec.tlsKey) &&
                Objects.equals(tlsCert, certSpec.tlsCert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, secretName, tlsKey, tlsCert);
    }

    @Override
    public String toString() {
        return "CertSpec{" +
                "provider='" + provider + '\'' +
                ", secretName='" + secretName + '\'' +
                '}';
    }
}
