/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import static java.util.Optional.empty;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.controller.common.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class AddressFinalizerController extends AbstractFinalizeController {

    private static final Logger log = LoggerFactory.getLogger(AddressFinalizerController.class.getName());

    private static final String ANNOTATION_ADDRESS_SPACE = "addressSpace";

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
     * @param addresses
     *
     * @param address The address to process.
     */
    private void processAddress(final String addressSpace, final Address address) {

        if (matchesAddressSpace(addressSpace, address)) {
            this.addressClient
                    .inNamespace(address.getMetadata().getNamespace())
                    .delete(address);
        }

    }

    private boolean matchesAddressSpace(final String addressSpace, final Address address) {

        return matchesAddressSpaceByAnnotation(addressSpace, address)
                .or(() -> matchesAddressSpaceByName(addressSpace, address))
                .orElse(false);

    }

    /**
     * Check if the address is part of the address space, by matching the annotation.
     *
     * @param addressSpace The address space to test for.
     * @param address The address to test.
     * @return If the "addressSpace" annotation is missing, {@link Optional#empty()} is being returned.
     *         Otherwise
     *         {@code true} will be returned if value of the annotation matches the address space name,
     *         or {@code false} otherwise.
     */
    private Optional<Boolean> matchesAddressSpaceByAnnotation(final String addressSpace, final Address address) {

        final ObjectMeta metadata = address.getMetadata();
        if (metadata == null) {
            return empty();
        }

        final Map<String, String> annotations = metadata.getAnnotations();
        if (annotations == null) {
            return empty();
        }

        final String addressSpaceName = annotations.get(ANNOTATION_ADDRESS_SPACE);
        if (addressSpaceName == null) {
            return empty();
        }

        return Optional.of(addressSpaceName.equals(addressSpace));

    }

    /**
     * Check if the address is part of the address space, by matching the annotation.
     *
     * @param addressSpace The address space to test for.
     * @param address The address to test.
     * @return {@code true} if the name of the address starts with the addressSpace name plus the ".".
     *         Never returns and {@link Optional#empty()}.
     */
    private Optional<Boolean> matchesAddressSpaceByName(final String addressSpace, final Address address) {
        return Optional.of(address.getMetadata().getName().startsWith(addressSpace + "."));
    }

    public String toString() {
        return AddressFinalizerController.class.getSimpleName();
    }
}
