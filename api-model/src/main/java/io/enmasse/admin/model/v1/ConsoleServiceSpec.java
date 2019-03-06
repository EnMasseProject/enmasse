/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({"discoveryMetadataURL", "oauthClientSecret", "certificateSecret",
        "scope"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleServiceSpec extends AbstractWithAdditionalProperties {

    private String discoveryMetadataURL;
    private SecretReference oauthClientSecret;
    private SecretReference certificateSecret;
    private String scope;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsoleServiceSpec that = (ConsoleServiceSpec) o;
        return Objects.equals(discoveryMetadataURL, that.discoveryMetadataURL) &&
                Objects.equals(oauthClientSecret, that.oauthClientSecret) &&
                Objects.equals(certificateSecret, that.certificateSecret) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discoveryMetadataURL, oauthClientSecret, certificateSecret, scope);
    }

    @Override
    public String toString() {
        return "ConsoleServiceSpec{" +
                "discoveryMetadataURL='" + discoveryMetadataURL + '\'' +
                ", oauthClientSecret=" + oauthClientSecret +
                ", certificateSecret=" + certificateSecret +
                ", scope='" + scope + '\'' +
                '}';
    }

    public String getDiscoveryMetadataURL() {
        return discoveryMetadataURL;
    }

    public void setDiscoveryMetadataURL(String discoveryMetadataURL) {
        this.discoveryMetadataURL = discoveryMetadataURL;
    }

    public SecretReference getOauthClientSecret() {
        return oauthClientSecret;
    }

    public void setOauthClientSecret(SecretReference oauthClientSecret) {
        this.oauthClientSecret = oauthClientSecret;
    }

    public SecretReference getCertificateSecret() {
        return certificateSecret;
    }

    public void setCertificateSecret(SecretReference certificateSecret) {
        this.certificateSecret = certificateSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
