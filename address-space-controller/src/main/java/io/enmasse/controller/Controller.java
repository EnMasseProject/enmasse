/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;

import java.util.List;

public interface Controller {
    default AddressSpace reconcile(AddressSpace addressSpace) throws Exception { return addressSpace; }
    default void reconcileAll(List<AddressSpace> addressSpaces) throws Exception { }

    static boolean isDeleted(final AddressSpace addressSpace) {
        return addressSpace.getMetadata().getDeletionTimestamp() != null
                && !addressSpace.getMetadata().getDeletionTimestamp().isBlank();
    }
}
