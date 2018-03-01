/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.NamespaceInfo;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.Watch;
import io.enmasse.k8s.api.Watcher;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceDeleteFailed;
import static io.enmasse.controller.common.ControllerReason.AddressSpaceDeleted;
import static io.enmasse.controller.common.ControllerReason.AddressSpaceSyncFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * The main controller loop that monitors k8s address spaces
 */
public class ControllerChain extends AbstractVerticle implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(ControllerChain.class.getName());

    private final Kubernetes kubernetes;
    private final AddressSpaceApi addressSpaceApi;

    private Watch watch;

    private final List<Controller> chain = new ArrayList<>();
    private final EventLogger eventLogger;

    public ControllerChain(Kubernetes kubernetes,
                           AddressSpaceApi addressSpaceApi,
                           EventLogger eventLogger) {
        this.kubernetes = kubernetes;
        this.addressSpaceApi = addressSpaceApi;
        this.eventLogger = eventLogger;
    }

    public ControllerChain addController(Controller controller) {
        chain.add(controller);
        return this;
    }

    @Override
    public void start(Future<Void> startPromise) {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(addressSpaceApi.watchAddressSpaces(this));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                this.watch = result.result();
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        vertx.executeBlocking(promise -> {
            try {
                if (watch != null) {
                    watch.close();
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                stopFuture.complete();
            } else {
                stopFuture.fail(result.cause());
            }
        });
    }

    @Override
    public void resourcesUpdated(Set<AddressSpace> resources) throws Exception {
        log.debug("Check standard address spaces: " + resources);

        for (AddressSpace addressSpace : resources) {
            try {
                for (Controller controller : chain) {
                    addressSpace = controller.handle(addressSpace);
                }

                addressSpaceApi.replaceAddressSpace(addressSpace);
            } catch (KubernetesClientException e) {
                log.warn("Error syncing address space {}", addressSpace.getName(), e);
                eventLogger.log(AddressSpaceSyncFailed, "Error syncing address space: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getName());
            }
        }
        retainAddressSpaces(resources);
    }

    private void retainAddressSpaces(Set<AddressSpace> desiredAddressSpaces) {
        if (desiredAddressSpaces.size() == 1 && desiredAddressSpaces.iterator().next().getNamespace().equals(kubernetes.getNamespace())) {
            return;
        }
        Set<NamespaceInfo> actual = kubernetes.listAddressSpaces();
        Set<NamespaceInfo> desired = desiredAddressSpaces.stream()
                .map(space -> new NamespaceInfo(space.getUid(), space.getName(), space.getNamespace(), space.getCreatedBy()))
                .collect(Collectors.toSet());

        actual.removeAll(desired);

        for (NamespaceInfo toRemove : actual) {
            try {
                kubernetes.deleteNamespace(toRemove);
                eventLogger.log(AddressSpaceDeleted, "Deleted address space", Normal, ControllerKind.AddressSpace, toRemove.getAddressSpace());
            } catch (KubernetesClientException e) {
                eventLogger.log(AddressSpaceDeleteFailed, "Error deleting namespace (may already be in progress): " + e.getMessage(), Warning, ControllerKind.AddressSpace, toRemove.getAddressSpace());
            }
        }
    }
}
