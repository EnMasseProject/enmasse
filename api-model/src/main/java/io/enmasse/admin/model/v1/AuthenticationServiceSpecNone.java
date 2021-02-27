/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.SecretReference;
import io.sundr.builder.annotations.Buildable;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonPropertyOrder({"certificateSecret"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationServiceSpecNone extends AbstractWithAdditionalProperties {
    private SecretReference certificateSecret;
    private Integer replicas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceSpecNone that = (AuthenticationServiceSpecNone) o;
        return Objects.equals(certificateSecret, that.certificateSecret) &&
                Objects.equals(replicas, that.replicas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificateSecret, replicas);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceSpecNone{" +
                "certificateSecret=" + certificateSecret +
                ", replicas=" + replicas +
                '}';
    }

    public SecretReference getCertificateSecret() {
        return certificateSecret;
    }

    public void setCertificateSecret(SecretReference certificateSecret) {
        this.certificateSecret = certificateSecret;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
