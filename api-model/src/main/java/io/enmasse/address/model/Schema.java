/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.common.model.AbstractHasMetadata;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

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
public class Schema {
    private List<@Valid AddressSpaceType> addressSpaceTypes = new ArrayList<>();
    private String creationTimestamp;

    public Schema() {
    }

    public Schema(List<AddressSpaceType> addressSpaceTypes, String creationTimestamp) {
        this.addressSpaceTypes = addressSpaceTypes;
        this.creationTimestamp = creationTimestamp;
    }

    public void setAddressSpaceTypes(List<AddressSpaceType> addressSpaceTypes) {
        this.addressSpaceTypes = addressSpaceTypes;
    }

    public List<AddressSpaceType> getAddressSpaceTypes() {
        return Collections.unmodifiableList(addressSpaceTypes);
    }

    public Optional<AddressSpaceType> findAddressSpaceType(String name) {
        for (AddressSpaceType type : addressSpaceTypes) {
            if (type.getName().equals(name)) {
                return Optional.ofNullable(type);
            }
        }
        return Optional.empty();
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }
}
