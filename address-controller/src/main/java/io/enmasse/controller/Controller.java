/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.SecretCertProvider;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.auth.AuthController;
import io.enmasse.controller.common.AuthenticationServiceResolverFactory;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceSyncFailed;

/**
 * The main controller loop that monitors k8s address spaces
 */
public class Controller extends AbstractVerticle implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(Controller.class.getName());
    private final OpenShiftClient client;

    private final AddressSpaceApi addressSpaceApi;

    private Watch watch;

    private final ControllerHelper helper;
    private final EventLogger eventLogger;
    private final AuthController authController;

    public Controller(OpenShiftClient client,
                      AddressSpaceApi addressSpaceApi,
                      Kubernetes kubernetes,
                      AuthenticationServiceResolverFactory authResolverFactory,
                      EventLogger eventLogger,
                      AuthController authController,
                      SchemaApi schemaApi) {
        this.helper = new ControllerHelper(kubernetes, authResolverFactory, eventLogger, schemaApi);
        this.client = client;
        this.addressSpaceApi = addressSpaceApi;
        this.eventLogger = eventLogger;
        this.authController = authController;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
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
    public void stop(Future<Void> stopFuture) throws Exception {
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
        createAddressSpaces(resources);
        retainAddressSpaces(resources);

        try {
            for (AddressSpace instance : addressSpaceApi.listAddressSpaces()) {
                AddressSpace.Builder mutableAddressSpace = new AddressSpace.Builder(instance);
                updateReadiness(mutableAddressSpace);
                updateEndpoints(mutableAddressSpace);
                try {
                    addressSpaceApi.replaceAddressSpace(mutableAddressSpace.build());
                } catch (KubernetesClientException e) {
                    log.warn("Error syncing address space {}", mutableAddressSpace.getName(), e);
                    eventLogger.log(AddressSpaceSyncFailed, "Error syncing address space: " + e.getMessage(), EventLogger.Type.Warning, ControllerKind.AddressSpace, mutableAddressSpace.getName());
                }

                if (authController != null) {
                    authController.issueAddressSpaceCert(instance);
                    authController.issueComponentCertificates(instance);
                    authController.issueExternalCertificates(instance);
                }
            }
        } catch (Exception e) {
            eventLogger.log(AddressSpaceSyncFailed, "Error syncing address space: " + e.getMessage(), EventLogger.Type.Warning, ControllerKind.Controller, "enmasse-controller");
            throw e;
        }
    }

    private void retainAddressSpaces(Set<AddressSpace> desiredAddressSpaces) {
        helper.retainAddressSpaces(desiredAddressSpaces);
    }

    private void createAddressSpaces(Set<AddressSpace> instances) {
        for (AddressSpace instance : instances) {
            helper.create(instance);
        }
    }

    private void updateEndpoints(AddressSpace.Builder builder) throws IOException {

        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.ADDRESS_SPACE, builder.getName());

        List<Endpoint> endpoints;
        /* Watch for routes and lb services */
        if (client.isAdaptable(OpenShiftClient.class)) {
            endpoints = client.routes().inNamespace(builder.getNamespace()).list().getItems().stream()
                    .filter(route -> isPartOfAddressSpace(builder.getName(), route))
                    .map(this::routeToEndpoint)
                    .collect(Collectors.toList());
        } else {
            endpoints = client.services().inNamespace(builder.getNamespace()).withLabel(LabelKeys.TYPE, "loadbalancer").list().getItems().stream()
                    .filter(service -> isPartOfAddressSpace(builder.getName(), service))
                    .map(this::serviceToEndpoint)
                    .collect(Collectors.toList());
        }

        log.debug("Updating endpoints for " + builder.getName() + " to " + endpoints);
        builder.setEndpointList(endpoints);
    }

    private static boolean isPartOfAddressSpace(String id, HasMetadata resource) {
        return resource.getMetadata().getAnnotations() != null && id.equals(resource.getMetadata().getAnnotations().get(AnnotationKeys.ADDRESS_SPACE));
    }

    private Endpoint routeToEndpoint(Route route) {
        String secretName = route.getMetadata().getAnnotations().get(AnnotationKeys.CERT_SECRET_NAME);
        Endpoint.Builder builder = new Endpoint.Builder()
                .setName(route.getMetadata().getName())
                .setHost(route.getSpec().getHost())
                .setPort(443)
                .setService(route.getSpec().getTo().getName());

        if (secretName != null) {
            builder.setCertProvider(new SecretCertProvider(secretName));
        }

        return builder.build();
    }

    private Endpoint serviceToEndpoint(Service service) {
        String secretName = service.getMetadata().getAnnotations().get(AnnotationKeys.CERT_SECRET_NAME);
        String serviceName = service.getMetadata().getAnnotations().get(AnnotationKeys.SERVICE_NAME);
        Endpoint.Builder builder = new Endpoint.Builder()
                .setName(service.getMetadata().getName())
                .setService(serviceName);

        if (secretName != null) {
            builder.setCertProvider(new SecretCertProvider(secretName));
        }

        if (service.getSpec().getPorts().size() > 0) {
            Integer nodePort = service.getSpec().getPorts().get(0).getNodePort();
            Integer port = service.getSpec().getPorts().get(0).getPort();

            if (nodePort != null) {
                builder.setPort(nodePort);
            } else if (port != null) {
                builder.setPort(port);
            }
        }

        return builder.build();
    }

    private void updateReadiness(AddressSpace.Builder mutableAddressSpace) {
        AddressSpace instance = mutableAddressSpace.build();
        boolean isReady = helper.isReady(instance);
        if (mutableAddressSpace.getStatus().isReady() != isReady) {
            mutableAddressSpace.getStatus().setReady(isReady);
        }
    }
}
