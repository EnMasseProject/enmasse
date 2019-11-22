/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ContinuationResult;

public class AddressFinalizerController extends AbstractFinalizerController {

    private static final Logger log = LoggerFactory.getLogger(AddressFinalizerController.class);

    public static final String FINALIZER_ADDRESSES = "enmasse.io/addresses";

    private static final Integer BATCH_SIZE = Integer.getInteger("io.enmasse.controller.AddressFinalizerController.batchSize", 100);

    private AddressSpaceApi addressSpaceApi;

    public AddressFinalizerController(final AddressSpaceApi addressSpaceApi) {
        super(FINALIZER_ADDRESSES);
        this.addressSpaceApi = addressSpaceApi;
    }

    @Override
    public Result processFinalizer(final AddressSpace addressSpace) {

        log.info("Processing address finalizer for {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());

        final String addressSpaceNamespace = addressSpace.getMetadata().getNamespace();
        final AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        ContinuationResult<Address> list = null;
        do {
            try {
                list = addressApi.listAddresses(addressSpaceNamespace, BATCH_SIZE, list, null);
            } catch (KubernetesClientException e) {
                // If not found, the address CRD does not exist so we drop the finalizer
                if (e.getCode() == 404) {
                    log.warn("Got 404 when listing addresses for {}/{}. Marking as finalized.", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), e);
                    return Result.completed(addressSpace);
                } else {
                    log.warn("Error finalizing {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), e);
                    return Result.waiting(addressSpace);
                }
            }

            log.debug("Processing addresses - found: {} -> continue: '{}'", list.getItems().size(), list.getContinuation());

            for (final Address address : list.getItems()) {
                processAddress(addressSpace, address);
            }

            if (!list.canContinue()) {
                log.debug("Completed cleanup");
                return Result.completed(addressSpace);
            }

        } while (true);

    }

    /**
     * Process a single address.
     *
     * @param addressSpace The address space to clean up.
     * @param address The address to process.
     */
    private void processAddress(final AddressSpace addressSpace, final Address address) {

        final AddressApi addressApi = this.addressSpaceApi.withAddressSpace(addressSpace);

        if (matchesAddressSpace(addressSpace.getMetadata().getName(), address)) {
            addressApi.deleteAddress(address);
        }

    }

    /**
     * Check if the address is part of the address space, by matching the annotation.
     *
     * @param addressSpace The address space to test for.
     * @param address The address to test.
     * @return {@code true} if the name of the address starts with the addressSpace name plus the ".".
     */
    private boolean matchesAddressSpace(final String addressSpace, final Address address) {
        return address.getMetadata().getName().startsWith(addressSpace + ".");
    }

    public String toString() {
        return AddressFinalizerController.class.getSimpleName();
    }
}
