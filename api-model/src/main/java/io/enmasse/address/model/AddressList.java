/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.Set;

/**
 * Type for address lists.
 */
public class AddressList extends ArrayList<Address> {
    public AddressList() {
        super();
    }

    public AddressList(Set<Address> addresses) {
        super(addresses);
    }
}
