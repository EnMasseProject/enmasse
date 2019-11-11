/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.controller.common.KubernetesHelper;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class AddressFinalizerController extends AbstractFinalizeController {

    private static final Logger log = LoggerFactory.getLogger(AddressFinalizerController.class.getName());

    private static final String FINALIZER_ADDRESSES = "enmasse.io/addresses";

    private static final Integer BATCH_SIZE = Integer.getInteger("io.enmasse.controller.AddressFinalizerController.batchSize", 100);

    private MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> addressClient;

    public AddressFinalizerController(NamespacedKubernetesClient controllerClient) {
        super(FINALIZER_ADDRESSES);
        this.addressClient = KubernetesHelper.clientForAddress(controllerClient);
    }

    @Override
    public Result processFinalizer(final AddressSpace addressSpace) {

        var addressSpaceName = addressSpace.getMetadata().getName();
        var addresses = this.addressClient.inNamespace(addressSpace.getMetadata().getNamespace());

        log.info("Processing finalizer for {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());

        AddressList list;
        String continueValue = null;
        do {
            list = addresses.list(BATCH_SIZE, continueValue);

            log.debug("Processing addresses - found: {} -> continue: '{}'", list.getItems().size(), list.getMetadata().getContinue());

            for (final Address address : list.getItems()) {
                processAddress(addressSpaceName, address);
            }

            continueValue = list.getMetadata().getContinue();
            if (continueValue == null || continueValue.isBlank()) {
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
    private void processAddress(final String addressSpace, final Address address) {

        if (matchesAddressSpace(addressSpace, address)) {
            this.addressClient
                    .inNamespace(address.getMetadata().getNamespace())
                    .delete(address);
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
