/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.admin.model.v1;

import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadataWithAdditionalProperties.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@DefaultCustomResource
@SuppressWarnings("serial")
public class AuthenticationService extends AbstractHasMetadataWithAdditionalProperties<AuthenticationService> {

    public static final String KIND = "AuthenticationService";

    private AuthenticationServiceSpec spec;
    private AuthenticationServiceStatus status;

    public AuthenticationService() {
        super(KIND, AdminCrd.API_VERSION_V1BETA1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationService that = (AuthenticationService) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec);
    }

    @Override
    public String toString() {
        return "AuthenticationService{" +
                "metadata='" + getMetadata() + '\'' +
                ", spec ='" + spec + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public void setSpec(AuthenticationServiceSpec spec) {
        this.spec = spec;
    }

    public AuthenticationServiceSpec getSpec() {
        return spec;
    }

    public void setStatus(AuthenticationServiceStatus status) {
        this.status = status;
    }

    public AuthenticationServiceStatus getStatus() {
        return status;
    }
}
