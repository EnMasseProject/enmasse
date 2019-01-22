/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.stream.Collectors;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpaceSchemaList extends AbstractList<AddressSpaceSchema> {

    public static final String KIND = "AddressSpaceSchemaList";

    public AddressSpaceSchemaList () {
        super(KIND, CoreCrd.API_VERSION);
    }

    public AddressSpaceSchemaList(Schema schema) {
        this();
        setItems(schema.getAddressSpaceTypes().stream()
                        .map(s -> {
                            return new AddressSpaceSchemaBuilder()
                                            .withNewMetadata()
                                            .withName(s.getName())
                                            .withCreationTimestamp(schema.getCreationTimestamp())
                                            .endMetadata()

                                            .withSpec(s)
                                            .build();
                        })
                        .collect(Collectors.toList()));
    }
}
