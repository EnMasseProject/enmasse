/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.SecretReference;
import io.sundr.builder.annotations.Buildable;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonPropertyOrder({"credentialsSecret", "certificateSecret", "storage", "securityContext"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationServiceSpecStandard extends AbstractWithAdditionalProperties {
    private SecretReference credentialsSecret;
    private SecretReference certificateSecret;
    private AuthenticationServiceSpecStandardStorage storage;
    private PodSecurityContext securityContext;
    private Integer replicas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationServiceSpecStandard that = (AuthenticationServiceSpecStandard) o;
        return Objects.equals(credentialsSecret, that.credentialsSecret) &&
                Objects.equals(certificateSecret, that.certificateSecret) &&
                Objects.equals(storage, that.storage) &&
                Objects.equals(securityContext, that.securityContext) &&
                Objects.equals(replicas, that.replicas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialsSecret, certificateSecret, storage, securityContext, replicas);
    }

    @Override
    public String toString() {
        return "AuthenticationServiceSpecStandard{" +
                "credentialsSecret=" + credentialsSecret +
                ", certificateSecret=" + certificateSecret +
                ", storage=" + storage +
                ", securityContext=" + securityContext +
                ", replicas=" + replicas +
                '}';
    }

    public SecretReference getCredentialsSecret() {
        return credentialsSecret;
    }

    public void setCredentialsSecret(SecretReference credentialsSecret) {
        this.credentialsSecret = credentialsSecret;
    }

    public SecretReference getCertificateSecret() {
        return certificateSecret;
    }

    public void setCertificateSecret(SecretReference certificateSecret) {
        this.certificateSecret = certificateSecret;
    }

    public AuthenticationServiceSpecStandardStorage getStorage() {
        return storage;
    }

    public void setStorage(AuthenticationServiceSpecStandardStorage storage) {
        this.storage = storage;
    }

    public PodSecurityContext getSecurityContext() {
        return securityContext;
    }

    public void setSecurityContext(PodSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
