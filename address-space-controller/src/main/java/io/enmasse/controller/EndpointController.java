/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointStatus;
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
        updateEndpoints(addressSpace);
        return addressSpace;
    }

    private void updateEndpoints(AddressSpace addressSpace) {

        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName());

        List<EndpointStatus> endpoints;
        /* Watch for routes and lb services */
        if (client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            endpoints = openShiftClient.routes().inNamespace(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE)).list().getItems().stream()
                    .filter(route -> isPartOfAddressSpace(addressSpace.getName(), route))
                    .map(route -> routeToEndpoint(addressSpace, route))
                    .collect(Collectors.toList());
        } else {
            endpoints = client.services().inNamespace(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE)).withLabel(LabelKeys.TYPE, "loadbalancer").list().getItems().stream()
                    .filter(service -> isPartOfAddressSpace(addressSpace.getName(), service))
                    .map(service -> serviceToEndpoint(addressSpace, service))
                    .collect(Collectors.toList());
        }

        log.debug("Updating endpoints for " + addressSpace.getName() + " to " + endpoints);
        addressSpace.getStatus().setEndpointStatuses(endpoints);
    }

    private static boolean isPartOfAddressSpace(String id, HasMetadata resource) {
        return resource.getMetadata().getAnnotations() != null && id.equals(resource.getMetadata().getAnnotations().get(AnnotationKeys.ADDRESS_SPACE));
    }

    private EndpointStatus routeToEndpoint(AddressSpace addressSpace, Route route) {
        Map<String, String> annotations = route.getMetadata().getAnnotations();

        String serviceName = annotations.get(AnnotationKeys.SERVICE_NAME);
        String endpointName = annotations.get(AnnotationKeys.ENDPOINT);

        Map<String, Integer> servicePorts = new HashMap<>();
        for (String annotationKey : annotations.keySet()) {
            if (annotationKey.startsWith(AnnotationKeys.SERVICE_PORT_PREFIX)) {
                String portName = annotationKey.substring(AnnotationKeys.SERVICE_PORT_PREFIX.length());
                int portValue = Integer.parseInt(annotations.get(annotationKey));
                servicePorts.put(portName, portValue);
            }
        }

        EndpointStatus.Builder builder = new EndpointStatus.Builder()
                .setName(endpointName)
                .setHost(route.getSpec().getHost())
                .setPort(443)
                .setServiceHost(serviceName + "." + addressSpace.getAnnotation(AnnotationKeys.NAMESPACE) + ".svc")
                .setServicePorts(servicePorts);

        return builder.build();
    }

    private EndpointStatus serviceToEndpoint(AddressSpace addressSpace, Service service) {
        String serviceName = service.getMetadata().getAnnotations().get(AnnotationKeys.SERVICE_NAME);
        String endpointName = service.getMetadata().getAnnotations().get(AnnotationKeys.ENDPOINT);
        EndpointStatus.Builder builder = new EndpointStatus.Builder()
                .setName(endpointName)
                .setServiceHost(serviceName + "." + addressSpace.getAnnotation(AnnotationKeys.NAMESPACE) + ".svc");

        if (service.getSpec().getPorts().size() > 0) {
            Integer nodePort = service.getSpec().getPorts().get(0).getNodePort();
            Integer port = service.getSpec().getPorts().get(0).getPort();

            builder.setHost(service.getSpec().getLoadBalancerIP());
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
