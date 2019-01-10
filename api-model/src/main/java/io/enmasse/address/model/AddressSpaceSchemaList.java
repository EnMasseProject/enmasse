/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.ListMeta;

import java.util.*;
import java.util.stream.Collectors;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "items"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpaceSchemaList {
    private final String apiVersion = "admin.enmasse.io/v1beta1";
    private final String kind = "AddressSpaceSchemaList";
    private final ListMeta metadata = new ListMeta();
    private final List<AddressSpaceSchema> items;

    public AddressSpaceSchemaList(Schema schema) {
        this.items = schema.getAddressSpaceTypes().stream()
                .map(s -> new AddressSpaceSchema(s, schema.getCreationTimestamp()))
                .collect(Collectors.toList());
    }

    public ListMeta getMetadata() {
        return metadata;
    }

    public List<AddressSpaceSchema> getItems() {
        return items;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getKind() {
        return kind;
    }
}
