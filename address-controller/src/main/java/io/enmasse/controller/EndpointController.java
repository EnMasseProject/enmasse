/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.Endpoint;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EndpointController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(EndpointController.class.getName());
    private final KubernetesClient client;

    public EndpointController(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) {
        AddressSpace.Builder builder = new AddressSpace.Builder(addressSpace);
        updateEndpoints(builder);
        return builder.build();
    }

    private void updateEndpoints(AddressSpace.Builder builder) {

        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.ADDRESS_SPACE, builder.getName());

        List<Endpoint> endpoints;
        /* Watch for routes and lb services */
        if (client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            endpoints = openShiftClient.routes().inNamespace(builder.getNamespace()).list().getItems().stream()
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
        String providerName = route.getMetadata().getAnnotations().get(AnnotationKeys.CERT_PROVIDER);
        String secretName = route.getMetadata().getAnnotations().get(AnnotationKeys.CERT_SECRET_NAME);
        Endpoint.Builder builder = new Endpoint.Builder()
                .setName(route.getMetadata().getName())
                .setHost(route.getSpec().getHost())
                .setPort(443)
                .setService(route.getSpec().getTo().getName());

        if (secretName != null) {
            builder.setCertSpec(new CertSpec(providerName, secretName));
        }

        return builder.build();
    }

    private Endpoint serviceToEndpoint(Service service) {
        String providerName = service.getMetadata().getAnnotations().get(AnnotationKeys.CERT_PROVIDER);
        String secretName = service.getMetadata().getAnnotations().get(AnnotationKeys.CERT_SECRET_NAME);
        String serviceName = service.getMetadata().getAnnotations().get(AnnotationKeys.SERVICE_NAME);
        Endpoint.Builder builder = new Endpoint.Builder()
                .setName(service.getMetadata().getName())
                .setService(serviceName);

        if (secretName != null) {
            builder.setCertSpec(new CertSpec(providerName, secretName));
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

    @Override
    public String toString() {
        return "EndpointController";
    }
}
