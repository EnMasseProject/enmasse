/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collection;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

/**
 * Type for address space list
 */
@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpaceList extends AbstractList<AddressSpace> {

    public static final String KIND = "AddressSpaceList";

    public AddressSpaceList() {
        super(KIND, CoreCrd.API_VERSION);
    }

    public AddressSpaceList(Collection<AddressSpace> addressSpaces) {
        this();
        setItems(addressSpaces);
    }
}
