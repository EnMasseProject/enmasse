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
public class UserMetadata {
    private final String name;
    private final String namespace;
    private final String creationTimestamp;
    private final String selfLink;

    @JsonCreator
    public UserMetadata(@JsonProperty("name") String name,
                        @JsonProperty("namespace") String namespace,
                        @JsonProperty("creationTimestamp") String creationTimestamp,
                        @JsonProperty("selfLink") String selfLink) {
        this.name = name;
        this.namespace = namespace;
        this.creationTimestamp = creationTimestamp;
        this.selfLink = selfLink;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public static class Builder {
        private String name;
        private String namespace;
        private String creationTimestamp;
        private String selfLink;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder setCreationTimestamp(String creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public Builder setSelfLink(String selfLink) {
            this.selfLink = selfLink;
            return this;
        }

        public UserMetadata build() {
            Objects.requireNonNull(name);
            Objects.requireNonNull(namespace);
            return new UserMetadata(name, namespace, creationTimestamp, selfLink);
        }
    }
}
