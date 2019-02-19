/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.common.model.AbstractHasMetadata;
import io.enmasse.model.validation.AuthenticationServiceDetails;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

/**
 * Represents an authentication service for an {@link AddressSpace}.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
@AuthenticationServiceDetails
public class AuthenticationService {

    private AuthenticationServiceType type;
    private Map<String, Object> details = new HashMap<> ();

    public AuthenticationService() {
    }

    public void setType(AuthenticationServiceType type) {
        this.type = type;
    }

    public AuthenticationServiceType getType() {
        return type;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Map<String, Object> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AuthenticationService status = (AuthenticationService) o;
        return type == status.type &&
                Objects.equals(details, status.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, details);
    }
}
