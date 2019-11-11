/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;

/**
 * Abstract base class for finalizer controllers.
 */
public abstract class AbstractFinalizeController implements Controller {

    private static final Logger log = LoggerFactory.getLogger(AbstractFinalizeController.class.getName());

    protected static interface Result {
        public AddressSpace getAddressSpace();

        public boolean isFinalized();

        public static Result waiting(final AddressSpace addressSpace) {
            return new Result() {
                @Override
                public boolean isFinalized() {
                    return false;
                }

                @Override
                public AddressSpace getAddressSpace() {
                    return addressSpace;
                }
            };
        }

        public static Result completed(final AddressSpace addressSpace) {
            return new Result() {
                @Override
                public boolean isFinalized() {
                    return true;
                }

                @Override
                public AddressSpace getAddressSpace() {
                    return addressSpace;
                }
            };
        }
    }

    protected final String id;

    public AbstractFinalizeController(final String id) {
        this.id = id;
    }

    /**
     * Process the finalizer logic.
     *
     * @param addressSpace The address space to work on.
     * @return The result of the process. Must never return {@code null}.
     */
    protected abstract Result processFinalizer(AddressSpace addressSpace);

    @Override
    public AddressSpace reconcile(final AddressSpace addressSpace) throws Exception {

        log.debug("Reconcile finalizer - id: {}, addressSpace: {}/{} -> {} ({})",
                this.id, addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), addressSpace.getMetadata().getDeletionTimestamp(),
                addressSpace.getMetadata().getFinalizers());

        // if we are not deleted ...
        if (!Controller.isDeleted(addressSpace)) {
            // ... we ensure we are added to the list of finalizers.
            return ensureFinalizer(addressSpace);
        }

        // if we are deleted, and no longer have the finalizer ...
        if (!addressSpace.getMetadata().getFinalizers().contains(this.id)) {
            // ... we have nothing to do.
            return addressSpace;
        }

        // process the finalizer
        final Result result = processFinalizer(addressSpace);

        // if we finished finalizing ...
        if (result == null || result.isFinalized()) {
            // ... remove ourselves from the list.
            return removeFinalizer(result.getAddressSpace());
        }

        // we still need to wait and will try again.
        return result.getAddressSpace();

    }

    /**
     * Remove the finalizer from the list.
     *
     * @param addressSpace The original address space.
     * @return A modified copy of the original, not having the finalizer set.
     */
    protected AddressSpace removeFinalizer(final AddressSpace addressSpace) {

        return new AddressSpaceBuilder(addressSpace)
                .editOrNewMetadata()
                .removeFromFinalizers(this.id)
                .endMetadata()
                .build();

    }

    /**
     * Ensure the finalizer is in the list.
     *
     * @param addressSpace The original address space.
     * @return A modified copy of the original, having the finalizer set.
     */
    protected AddressSpace ensureFinalizer(final AddressSpace addressSpace) {

        // if the finalizer list does contain our id ...
        if (addressSpace.getMetadata().getFinalizers().contains(this.id)) {
            // ... we are done.
            return addressSpace;
        }

        // ... otherwise we add ourselves to the list.
        return new AddressSpaceBuilder(addressSpace)
                .editOrNewMetadata()
                .addNewFinalizer(this.id)
                .endMetadata()
                .build();

    }

}
