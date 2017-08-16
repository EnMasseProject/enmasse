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
package io.enmasse.controller.standard;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.*;
import io.enmasse.controller.k8s.api.AddressSpaceApi;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.SecretCertProvider;
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
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class StandardController extends AbstractVerticle implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(StandardController.class.getName());
    private final OpenShiftClient client;

    private final StandardHelper helper;
    private final AddressSpaceApi addressSpaceApi;
    private Watch watch;

    private final Map<AddressSpace, String> addressControllerMap = new HashMap<>();
    private final Kubernetes kubernetes;

    public StandardController(OpenShiftClient client, AddressSpaceApi addressSpaceApi, Kubernetes kubernetes, AuthenticationServiceResolverFactory authResolverFactory, boolean isMultitenant) {
        this.helper = new StandardHelper(kubernetes, isMultitenant, authResolverFactory);
        this.client = client;
        this.addressSpaceApi = addressSpaceApi;
        this.kubernetes = kubernetes;
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
    public synchronized void resourcesUpdated(Set<AddressSpace> instances) throws Exception {
        log.debug("Check standard address spaces: " + instances);
        createAddressSpaces(instances);
        retainAddressSpaces(instances);

        for (AddressSpace instance : addressSpaceApi.listAddressSpaces()) {
            AddressSpace.Builder mutableAddressSpace = new AddressSpace.Builder(instance);
            updateReadiness(mutableAddressSpace);
            updateEndpoints(mutableAddressSpace);
            addressSpaceApi.replaceAddressSpace(mutableAddressSpace.build());
        }
        
        createAddressControllers(instances);
        deleteAddressControllers(instances);
    }

    private void createAddressControllers(Set<AddressSpace> addressSpaces) {
        for (AddressSpace addressSpace : addressSpaces) {
            if (!addressControllerMap.containsKey(addressSpace)) {
                AddressClusterGenerator clusterGenerator = new TemplateAddressClusterGenerator(addressSpaceApi, kubernetes);
                AddressController addressController = new AddressController(
                        addressSpaceApi.withAddressSpace(addressSpace),
                        kubernetes.withNamespace(addressSpace.getNamespace()),
                        clusterGenerator);
                log.info("Deploying address space controller for " + addressSpace.getName());
                vertx.deployVerticle(addressController, result -> {
                    if (result.succeeded()) {
                        addressControllerMap.put(addressSpace, result.result());
                    } else {
                        log.warn("Unable to deploy address controller for " + addressSpace.getName());
                    }
                });
            }
        }
    }

    private void deleteAddressControllers(Set<AddressSpace> addressSpaces) {
        Iterator<Map.Entry<AddressSpace, String>> it = addressControllerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AddressSpace, String> entry = it.next();
            if (!addressSpaces.contains(entry.getKey())) {
                vertx.undeploy(entry.getValue());
                it.remove();
            }
        }
    }

    private void retainAddressSpaces(Set<AddressSpace> desiredAddressSpaces) {
        helper.retainAddressSpaces(desiredAddressSpaces.stream().map(AddressSpace::getName).collect(Collectors.toSet()));
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
