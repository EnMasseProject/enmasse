/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.SecretCertProvider;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.auth.UserDatabase;
import io.enmasse.controller.common.AddressSpaceController;
import io.enmasse.controller.common.AuthenticationServiceResolverFactory;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.Watch;
import io.enmasse.k8s.api.Watcher;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main controller loop that monitors k8s address spaces
 */
public class Controller extends AbstractVerticle implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(Controller.class.getName());
    private final OpenShiftClient client;

    private final AddressSpaceApi addressSpaceApi;
    private Watch watch;

    private final List<AddressSpaceController> addressSpaceControllers;
    private final ControllerHelper helper;

    public Controller(OpenShiftClient client,
                      AddressSpaceApi addressSpaceApi,
                      Kubernetes kubernetes,
                      AuthenticationServiceResolverFactory authResolverFactory,
                      UserDatabase userDatabase,
                      List<AddressSpaceController> addressSpaceControllers) {
        this.helper = new ControllerHelper(kubernetes, authResolverFactory, userDatabase);
        this.client = client;
        this.addressSpaceApi = addressSpaceApi;
        this.addressSpaceControllers = addressSpaceControllers;
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

        for (AddressSpace instance : addressSpaceApi.listAddressSpaces()) {
            AddressSpace.Builder mutableAddressSpace = new AddressSpace.Builder(instance);
            updateReadiness(mutableAddressSpace);
            updateEndpoints(mutableAddressSpace);
            addressSpaceApi.replaceAddressSpace(mutableAddressSpace.build());
        }

        for (AddressSpaceController controller : addressSpaceControllers) {
            Set<AddressSpace> filtered = resources.stream()
                    .filter(space -> space.getType().getName().equals(controller.getAddressSpaceType().getName()))
                    .collect(Collectors.toSet());
            controller.resourcesUpdated(filtered);
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
        /* Watch for routes and ingress */
        if (client.isAdaptable(OpenShiftClient.class)) {
            endpoints = client.routes().inNamespace(builder.getNamespace()).list().getItems().stream()
                    .filter(route -> isPartOfAddressSpace(builder.getName(), route))
                    .map(this::routeToEndpoint)
                    .collect(Collectors.toList());
        } else {
            endpoints = client.extensions().ingresses().inNamespace(builder.getNamespace()).list().getItems().stream()
                    .filter(ingress -> isPartOfAddressSpace(builder.getName(), ingress))
                    .map(this::ingressToEndpoint)
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
                .setService(route.getSpec().getTo().getName());

        if (secretName != null) {
            builder.setCertProvider(new SecretCertProvider(secretName));
        }

        return builder.build();
    }

    private Endpoint ingressToEndpoint(Ingress ingress) {
        String secretName = ingress.getMetadata().getAnnotations().get(AnnotationKeys.CERT_SECRET_NAME);
        Endpoint.Builder builder = new Endpoint.Builder()
                .setName(ingress.getMetadata().getName())
                .setService(ingress.getSpec().getBackend().getServiceName());

        if (secretName != null) {
            builder.setCertProvider(new SecretCertProvider(secretName));
            if (ingress.getSpec().getTls() != null && !ingress.getSpec().getTls().isEmpty() &&
                    !ingress.getSpec().getTls().get(0).getHosts().isEmpty()) {
                builder.setHost(ingress.getSpec().getTls().get(0).getHosts().get(0));
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
