/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    @JsonProperty("apiVersion")
    private final String apiVersion = "user.enmasse.io/v1alpha1";
    @JsonProperty("kind")
    private final String kind = "MessagingUser";

    private final UserMetadata metadata;
    private final UserSpec spec;

    @JsonCreator
    public User(@JsonProperty("metadata") UserMetadata metadata,
                @JsonProperty("spec") UserSpec spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public UserMetadata getMetadata() {
        return metadata;
    }

    public UserSpec getSpec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(apiVersion, user.apiVersion) &&
                Objects.equals(kind, user.kind) &&
                Objects.equals(metadata, user.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, kind, metadata);
    }

    public void validate() {
        Objects.requireNonNull(metadata, "'metadata' must be set");
        Objects.requireNonNull(spec, "'spec' must be set");

        metadata.validate();
        spec.validate();
    }

    public static class Builder {
        private UserMetadata metadata;
        private UserSpec spec;

        public Builder() { }

        public Builder(User user) {
            this.metadata = user.getMetadata();
            this.spec = user.getSpec();
        }

        public Builder setMetadata(UserMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder setSpec(UserSpec spec) {
            this.spec = spec;
            return this;
        }

        public User build() {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(spec);
            return new User(metadata, spec);
        }
    }
}
