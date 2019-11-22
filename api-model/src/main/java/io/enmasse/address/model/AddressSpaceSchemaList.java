/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.List;
import java.util.stream.Collectors;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpaceSchemaList extends AbstractList<AddressSpaceSchema> {

    public static final String KIND = "AddressSpaceSchemaList";

    public AddressSpaceSchemaList() {
        super(KIND, CoreCrd.API_VERSION);
    }

    public static AddressSpaceSchemaList fromSchema(final Schema schema) {
        if (schema == null) {
            return null;
        }

        final AddressSpaceSchemaList list = new AddressSpaceSchemaList();
        final List<AddressSpaceSchema> items = schema.getAddressSpaceTypes().stream()
                .map(type -> AddressSpaceSchema.fromAddressSpaceType(type, schema.getAuthenticationServices()))
                .collect(Collectors.toList());

        list.setItems(items);

        return list;
    }
}
