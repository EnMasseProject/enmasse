/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;

import java.util.List;

public interface Controller {
    default AddressSpace reconcileActive(AddressSpace addressSpace) throws Exception {
        return addressSpace;
    }

    default AddressSpace reconcileAnyState(AddressSpace addressSpace) throws Exception {
        if (!Controller.isDeleted(addressSpace)) {
            return reconcileActive(addressSpace);
        } else {
            return addressSpace;
        }
    }

    default void reconcileAll(List<AddressSpace> addressSpaces) throws Exception {}

    /**
     * Test if an address space is in the process of being deleted.
     *
     * @param addressSpace The address space to test.
     * @return {@code true} if the address considered being deleted, {@code false} otherwise.
     */
    static boolean isDeleted(final AddressSpace addressSpace) {
        return addressSpace.getMetadata().getDeletionTimestamp() != null
                && !addressSpace.getMetadata().getDeletionTimestamp().isBlank();
    }
}
