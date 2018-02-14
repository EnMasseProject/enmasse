/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Type for address space list
 */
public class AddressSpaceList extends ArrayList<AddressSpace> {

    public AddressSpaceList() {
        super();
    }

    public AddressSpaceList(Collection<AddressSpace> addressSpaces) {
        super(addressSpaces);
    }
}
