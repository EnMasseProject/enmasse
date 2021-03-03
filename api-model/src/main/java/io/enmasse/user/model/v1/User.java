/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.enmasse.admin.model.v1.WithAdditionalProperties;
import io.enmasse.common.model.CustomResourceWithAdditionalProperties;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
        )
@DefaultCustomResource
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
@Version(UserCrd.VERSION)
@Group(UserCrd.GROUP)
@Kind(User.KIND)
public class User extends CustomResourceWithAdditionalProperties<UserSpec, UserStatus> implements WithAdditionalProperties, Namespaced {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z]+([a-z0-9\\-]*[a-z0-9]+|[a-z0-9]*)\\.[a-z0-9]+([a-z0-9@.\\-]*[a-z0-9]+|[a-z0-9]*)$");

    public static final String KIND = "MessagingUser";

    // for builders - probably will be fixed by https://github.com/fabric8io/kubernetes-client/pull/1346
    private ObjectMeta metadata;
    private UserSpec spec;
    private UserStatus status;

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public void setSpec(final UserSpec spec) {
        this.spec = spec;
    }

    public UserSpec getSpec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(getApiVersion(), user.getApiVersion()) &&
                Objects.equals(getKind(), user.getKind()) &&
                Objects.equals(getMetadata(), user.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApiVersion(), getKind(), getMetadata(), spec);
    }

    public void validate() {
        try {

            validateMetadata();

            Objects.requireNonNull(spec, "'spec' must be set");
            spec.validate();

        } catch (Exception e) {
            throw new UserValidationFailedException(e);
        }
    }

    private void validateMetadata() {
        Objects.requireNonNull(getMetadata(), "'metadata' must be set");

        final String name = getMetadata().getName();
        final String namespace = getMetadata().getNamespace();

        Objects.requireNonNull(name, "'name' must be set");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new UserValidationFailedException("Invalid resource name '" + name + "', must match " + NAME_PATTERN);
        }
        Objects.requireNonNull(namespace, "'namespace' must be set");

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("metadata=").append(getMetadata()).append(",");
        sb.append("spec=").append(spec);
        sb.append("status=").append(status);
        sb.append("}");
        return sb.toString();
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
