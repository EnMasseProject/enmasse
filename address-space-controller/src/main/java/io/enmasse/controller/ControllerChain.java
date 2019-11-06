/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.ResourceChecker;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.api.Watch;
import io.enmasse.k8s.api.Watcher;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceSyncFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * The main controller loop that monitors k8s address spaces
 */
public class ControllerChain implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(ControllerChain.class.getName());

    private final AddressSpaceApi addressSpaceApi;

    private Watch watch;

    private final List<Controller> chain = new ArrayList<>();
    private final SchemaProvider schemaProvider;
    private final EventLogger eventLogger;
    private final Duration recheckInterval;
    private final Duration resyncInterval;
    private ResourceChecker<AddressSpace> checker;

    public ControllerChain(AddressSpaceApi addressSpaceApi,
                           SchemaProvider schemaProvider,
                           EventLogger eventLogger,
                           Duration recheckInterval,
                           Duration resyncInterval) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.eventLogger = eventLogger;
        this.recheckInterval = recheckInterval;
        this.resyncInterval = resyncInterval;
    }

    public void addController(Controller controller) {
        chain.add(controller);
    }

    public void start() throws Exception {
        checker = new ResourceChecker<>(this, recheckInterval);
        checker.start();
        this.watch = addressSpaceApi.watchAddressSpaces(checker, resyncInterval);
    }

    public void stop() throws Exception {
        if (watch != null) {
            watch.close();
            watch = null;
        }
        if (checker != null) {
            checker.stop();
            checker = null;
        }
    }

    @Override
    public void onUpdate(List<AddressSpace> resources) throws Exception {
        log.info("Check address spaces: {}", resources.stream().map(a -> a.getMetadata().getNamespace()+":"+a.getMetadata().getName()).collect(Collectors.toSet()));

        if (schemaProvider.getSchema() == null) {
            log.info("No schema available");
            return;
        }

        List<AddressSpace> updatedResources = new ArrayList<>();

        for (AddressSpace addressSpace : resources) {

            try {
                AddressSpace original = new AddressSpaceBuilder(addressSpace).build();

                log.info("Checking address space {}:{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
                addressSpace.getStatus().setReady(true);
                addressSpace.getStatus().clearMessages();
                for (Controller controller : chain) {
                    log.info("Controller {}", controller);
                    log.debug("Address space input: {}", addressSpace);
                    addressSpace = controller.reconcile(addressSpace);
                }

                log.debug("Controller chain output: {}", addressSpace);

                if (hasAddressSpaceChanged(original, addressSpace)) {
                    if (!original.getMetadata().equals(addressSpace.getMetadata())) {
                        log.debug("Meta changed from {} to {}", original.getMetadata(), addressSpace.getMetadata());
                    }
                    if (!original.getSpec().equals(addressSpace.getSpec())) {
                        log.debug("Spec changed from {} to {}", original.getSpec(), addressSpace.getSpec());
                    }
                    if (!original.getStatus().equals(addressSpace.getStatus())) {
                        log.debug("Status changed from {} to {}", original.getStatus(), addressSpace.getStatus());
                    }
                    addressSpaceApi.replaceAddressSpace(addressSpace);
                }
            } catch (KubernetesClientException e) {
                log.warn("Error syncing address space {}", addressSpace.getMetadata().getName(), e);
                eventLogger.log(AddressSpaceSyncFailed, "Error syncing address space: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getMetadata().getName());
            } catch (Exception e) {
                log.warn("Error processing address space {}", addressSpace.getMetadata().getName(), e);
            } finally {
                updatedResources.add(addressSpace);
            }
        }

        for (Controller controller : chain) {
            try {
                controller.reconcileAll(updatedResources);
            } catch (Exception e) {
                log.warn("Exception in {} reconcileAll", controller, e);
            }
        }

    }

    private boolean hasAddressSpaceChanged(AddressSpace original, AddressSpace addressSpace) {
        return !(original.getMetadata().equals(addressSpace.getMetadata()) &&
                original.getSpec().equals(addressSpace.getSpec()) &&
                original.getStatus().equals(addressSpace.getStatus()));
    }
}
