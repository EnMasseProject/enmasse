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

    default ReconcileResult reconcileAnyState(AddressSpace addressSpace) throws Exception {
        if (!Controller.isDeleted(addressSpace)) {
            return ReconcileResult.create(reconcileActive(addressSpace));
        } else {
            return ReconcileResult.create(addressSpace);
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

    class ReconcileResult {
        private final AddressSpace addressSpace;
        private final boolean persistAndRequeue;

        private ReconcileResult(AddressSpace addressSpace, boolean persistAndRequeue) {
            this.addressSpace = addressSpace;
            this.persistAndRequeue = persistAndRequeue;
        }

        public AddressSpace getAddressSpace() {
            return addressSpace;
        }

        /**
         * Signal if the AddressSpace object return in this result should be persisted. It is assumed that the
         * AddressSpace object contains the modifications that should be persisted.
         */
        public boolean isPersistAndRequeue() {
            return persistAndRequeue;
        }

        public static ReconcileResult create(AddressSpace addressSpace) {
            return new ReconcileResult(addressSpace, false);
        }

        public static ReconcileResult createRequeued(AddressSpace addressSpace, boolean requeue) {
            return new ReconcileResult(addressSpace, requeue);
        }

    }
}
