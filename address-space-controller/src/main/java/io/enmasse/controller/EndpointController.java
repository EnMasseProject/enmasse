/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EndpointController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(EndpointController.class.getName());
    private final KubernetesClient client;
    private final boolean exposeServicesByDefault;

    public EndpointController(KubernetesClient client, boolean exposeServicesByDefault) {
        this.client = client;
        this.exposeServicesByDefault = exposeServicesByDefault;
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) {
        updateEndpoints(addressSpace);
        return addressSpace;
    }

    private void updateEndpoints(AddressSpace addressSpace) {

        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName());

        String namespace = addressSpace.getAnnotation(AnnotationKeys.NAMESPACE);
        List<Service> services = client.services().inNamespace(namespace).list().getItems();
        List<EndpointInfo> endpoints = collectEndpoints(addressSpace, services);

        /* Watch for routes and lb services */
        List<EndpointStatus> statuses;
        if (exposeServicesByDefault) {
            statuses = exposeEndpoints(addressSpace, endpoints);
        } else {
            statuses = endpoints.stream().map(e -> e.endpointStatus).collect(Collectors.toList());
        }

        log.debug("Updating endpoints for " + addressSpace.getName() + " to " + statuses);
        addressSpace.getStatus().setEndpointStatuses(statuses);
    }

    private static class EndpointInfo {
        private final EndpointSpec endpointSpec;
        private final EndpointStatus endpointStatus;

        private EndpointInfo(EndpointSpec endpointSpec, EndpointStatus endpointStatus) {
            this.endpointSpec = endpointSpec;
            this.endpointStatus = endpointStatus;
        }
    }

    public List<EndpointInfo> collectEndpoints(AddressSpace addressSpace, List<Service> services) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        String infraNamespace = addressSpace.getAnnotation(AnnotationKeys.NAMESPACE);

        for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
            EndpointStatus.Builder statusBuilder = new EndpointStatus.Builder();
            statusBuilder.setName(endpoint.getName());
            statusBuilder.setServiceHost(endpoint.getService() + "." + infraNamespace + ".svc");
            Service service = findService(services, endpoint.getService());
            if (service == null) {
                continue;
            }


            Map<String, Integer> servicePorts = new HashMap<>();
            Map<String, String> serviceAnnotations = service.getMetadata().getAnnotations();
            for (Map.Entry<String, String> annotationEntry : serviceAnnotations.entrySet()) {
                String annotationKey = annotationEntry.getKey();
                String annotationValue = annotationEntry.getValue();
                if (annotationKey.startsWith(AnnotationKeys.SERVICE_PORT_PREFIX)) {
                    String portName = annotationKey.substring(AnnotationKeys.SERVICE_PORT_PREFIX.length());
                    int portValue = Integer.parseInt(annotationValue);
                    servicePorts.put(portName, portValue);
                }
            }
            statusBuilder.setServicePorts(servicePorts);
            endpoints.add(new EndpointInfo(endpoint, statusBuilder.build()));
        }
        return endpoints;
    }

    private Service findService(List<Service> services, String serviceName) {
        for (Service service : services) {
            if (serviceName.equals(service.getMetadata().getName())) {
                return service;
            }
        }
        return null;
    }

    private List<EndpointStatus> exposeEndpoints(AddressSpace addressSpace, List<EndpointInfo> endpoints) {
        List<EndpointStatus> exposedStatuses = new ArrayList<>();

        for (EndpointInfo endpoint : endpoints) {
            EndpointStatus.Builder statusBuilder = new EndpointStatus.Builder(endpoint.endpointStatus);

            if (client.isAdaptable(OpenShiftClient.class)) {
                Route route = ensureRouteExists(addressSpace, endpoint.endpointSpec);
                statusBuilder.setPort(443);
                statusBuilder.setHost(route.getSpec().getHost());
            } else {
                Service service = ensureExternalServiceExists(addressSpace, endpoint.endpointSpec);
                if (service != null && service.getSpec().getPorts().size() > 0) {
                    Integer nodePort = service.getSpec().getPorts().get(0).getNodePort();
                    Integer port = service.getSpec().getPorts().get(0).getPort();

                    statusBuilder.setHost(service.getSpec().getLoadBalancerIP());
                    if (nodePort != null) {
                        statusBuilder.setPort(nodePort);
                    } else if (port != null) {
                        statusBuilder.setPort(port);
                    }
                }
            }
            exposedStatuses.add(statusBuilder.build());
        }

        return exposedStatuses;
    }

    private Route ensureRouteExists(AddressSpace addressSpace, EndpointSpec endpointSpec) {
        OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
        String infraNamespace = addressSpace.getAnnotation(AnnotationKeys.NAMESPACE);
        Route existingRoute = openShiftClient.routes().inNamespace(infraNamespace).withName(endpointSpec.getName()).get();
        if (existingRoute != null) {
            return existingRoute;
        }

        RouteBuilder route = new RouteBuilder()
                .editOrNewMetadata()
                .withName(endpointSpec.getName())
                .withNamespace(infraNamespace)
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                .addToAnnotations(AnnotationKeys.SERVICE_NAME, endpointSpec.getService())
                .endMetadata()
                .editOrNewSpec()
                .withHost(endpointSpec.getHost().orElse(""))
                .withNewTo()
                .withName(endpointSpec.getService())
                .withKind("Service")
                .endTo()
                .withNewPort()
                .editOrNewTargetPort()
                .withStrVal(endpointSpec.getServicePort())
                .endTargetPort()
                .endPort()
                .endSpec();

        if (endpointSpec.getCertSpec().isPresent()) {
            route.editOrNewSpec()
                    .withNewTls()
                    .withTermination("passthrough")
                    .endTls()
                    .endSpec();
        }

        return openShiftClient.routes().inNamespace(infraNamespace).create(route.build());
    }

    private Service ensureExternalServiceExists(AddressSpace addressSpace, EndpointSpec endpointSpec) {
        String infraNamespace = addressSpace.getAnnotation(AnnotationKeys.NAMESPACE);
        String serviceName = endpointSpec.getName() + "-external";

        Service existingService = client.services().inNamespace(infraNamespace).withName(serviceName).get();
        if (existingService != null) {
            return existingService;
        }

        Service service = client.services().inNamespace(infraNamespace).withName(endpointSpec.getService()).get();
        if (service == null) {
            return null;
        }


        ServicePort servicePort = null;
        for (ServicePort port : service.getSpec().getPorts()) {
            if (port.getName().equals(endpointSpec.getServicePort())) {
                servicePort = port;
                break;
            }
        }
        if (servicePort == null) {
            return null;
        }

        ServiceBuilder svc = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(serviceName)
                .withNamespace(infraNamespace)
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                .addToAnnotations(AnnotationKeys.SERVICE_NAME, endpointSpec.getService())
                .endMetadata()
                .editOrNewSpec()
                .withPorts(servicePort)
                .withSelector(service.getSpec().getSelector())
                .withType("LoadBalancer")
                .endSpec();

        return client.services().inNamespace(infraNamespace).create(svc.build());
    }

    @Override
    public String toString() {
        return "EndpointController";
    }
}
