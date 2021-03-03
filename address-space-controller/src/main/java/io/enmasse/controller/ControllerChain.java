/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceSyncFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.ResourceChecker;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.api.Watch;
import io.enmasse.k8s.api.Watcher;
import io.fabric8.kubernetes.client.KubernetesClientException;

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

        boolean requeue;
        do {
            List<AddressSpace> updatedResources = new ArrayList<>();
            requeue = false;
            for (final AddressSpace original : resources) {

                AddressSpace addressSpace = new AddressSpaceBuilder(original).build();
                final String addressSpaceName = addressSpace.getMetadata().getNamespace() + ":" + addressSpace.getMetadata().getName();

                try {

                    log.debug("Controller chain input: {}", original);

                    log.info("Checking address space : {} creationTimestamp: {} deletionTimestamp: {}", addressSpaceName, addressSpace.getMetadata().getCreationTimestamp(), addressSpace.getMetadata().getDeletionTimestamp());
                    for (Controller controller : chain) {
                        log.info("Controller {}", controller);
                        log.debug("Address space input: {}", addressSpace);
                        Controller.ReconcileResult result = controller.reconcileAnyState(addressSpace);
                        addressSpace = result.getAddressSpace();

                        // If instructed to requeue, break loop and let the comparison of original vs current
                        // determine if we need to persist anything. Signal that the reconcile loop should run
                        // again by setting requeue. This will refresh the current list of address spaces and rerun the
                        // reconcile loop with persisted finalizers.
                        if (result.isPersistAndRequeue()) {
                            requeue = true;
                            break;
                        }
                    }

                    log.debug("Controller chain output: {}", addressSpace);

                    if (hasAddressSpaceChanged(original, addressSpace)) {
                        log.debug("Change detected. Executing update.");
                        if (!this.addressSpaceApi.replaceAddressSpace(addressSpace)) {
                            log.info("Unable to persist address space state: {}", addressSpaceName);
                        }
                    } else {
                        log.debug("No change detected. Not triggering update.");
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
            if (requeue) {
                resources = new ArrayList<>(addressSpaceApi.listAllAddressSpaces());
            }
        } while(requeue);
    }

    static boolean hasAddressSpaceChanged(AddressSpace original, AddressSpace addressSpace) {

        boolean changed = false;

        if (!Objects.equals(original.getMetadata(), addressSpace.getMetadata())) {
            log.debug("Meta changed from {} to {}", original.getMetadata(), addressSpace.getMetadata());
            changed = true;
        }
        if (!Objects.equals(original.getSpec(), addressSpace.getSpec())) {
            log.debug("Spec changed from {} to {}", original.getSpec(), addressSpace.getSpec());
            changed = true;
        }
        if (!Objects.equals(original.getStatus(), addressSpace.getStatus())) {
            log.debug("Status changed from {} to {}", original.getStatus(), addressSpace.getStatus());
            changed = true;
        }

        return changed;
    }
}
