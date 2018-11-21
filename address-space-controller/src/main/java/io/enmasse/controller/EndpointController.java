/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class EndpointController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(EndpointController.class.getName());
    private final OpenShiftClient client;
    private final boolean exposeServicesByDefault;
    private final String namespace;

    public EndpointController(OpenShiftClient client, boolean exposeServicesByDefault) {
        this.client = client;
        this.exposeServicesByDefault = exposeServicesByDefault;
        namespace = client.getNamespace();
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) {
        updateEndpoints(addressSpace);
        return addressSpace;
    }

    private void updateEndpoints(AddressSpace addressSpace) {

        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName());

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        List<Service> services = client.services().inNamespace(namespace).withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems();
        List<EndpointInfo> endpoints = collectEndpoints(addressSpace, services);

        /* Watch for routes and lb services */

        final List<EndpointStatus> statuses;
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

        for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
            EndpointStatus.Builder statusBuilder = new EndpointStatus.Builder();
            statusBuilder.setName(endpoint.getName());
            statusBuilder.setServiceHost(KubeUtil.getAddressSpaceServiceHost(endpoint.getService(), namespace, addressSpace));
            Service service = findService(services, KubeUtil.getAddressSpaceServiceName(endpoint.getService(), addressSpace));
            if (service == null) {
                continue;
            }

            statusBuilder.setServicePorts(ServiceHelper.getServicePorts(service));
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

            if (endpoint.endpointSpec.getExposeSpec().isPresent()) {
                exposedStatuses.add(exposeEndpoint(addressSpace, endpoint, endpoint.endpointSpec.getExposeSpec().get()));
            }
        }

        return exposedStatuses;
    }

    private EndpointStatus exposeEndpoint(AddressSpace addressSpace, EndpointInfo endpointInfo, ExposeSpec exposeSpec) {
        EndpointStatus.Builder statusBuilder = new EndpointStatus.Builder(endpointInfo.endpointStatus);
        try {
            switch (exposeSpec.getType()) {
                case route:
                    Route route = ensureRouteExists(addressSpace, endpointInfo.endpointSpec, exposeSpec);
                    if (route != null) {
                        statusBuilder.setExternalPorts(Collections.singletonMap(exposeSpec.getRouteServicePort(), 443));
                        statusBuilder.setExternalHost(route.getSpec().getHost());
                        if (exposeSpec.getRouteTlsTermination().equals(ExposeSpec.TlsTermination.passthrough)) {
                            Secret certSecret = client.secrets().inNamespace(namespace).withName(KubeUtil.getExternalCertSecretName(endpointInfo.endpointSpec.getService(), addressSpace)).get();
                            if (certSecret != null) {
                                statusBuilder.setCertificate(certSecret.getData().get("tls.crt"));
                            }
                        } else {
                            statusBuilder.setCertificate(route.getSpec().getTls().getCertificate());
                        }
                    }
                    break;
                case loadbalancer:
                    Service service = ensureExternalServiceExists(addressSpace, endpointInfo.endpointSpec, exposeSpec);
                    if (service != null && service.getSpec().getPorts().size() > 0) {
                        statusBuilder.setExternalHost(service.getSpec().getLoadBalancerIP());
                        statusBuilder.setExternalPorts(endpointInfo.endpointStatus.getServicePorts());
                    }
                    break;
            }
        } catch (KubernetesClientException e) {
            String error = String.format("Error exposing endpoint %s: %s", endpointInfo.endpointSpec.getName(), e.getMessage());
            log.warn(error);
            addressSpace.getStatus().appendMessage(error);
        }
        return statusBuilder.build();
    }

    private Route ensureRouteExists(AddressSpace addressSpace, EndpointSpec endpointSpec, ExposeSpec exposeSpec) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        String routeName = KubeUtil.getAddressSpaceRouteName(endpointSpec.getName(), addressSpace);

        Route existingRoute = client.routes().inNamespace(namespace).withName(routeName).get();
        if (existingRoute != null) {
            return existingRoute;
        }

        String serviceName = KubeUtil.getAddressSpaceServiceName(endpointSpec.getService(), addressSpace);

        RouteBuilder route = new RouteBuilder()
                .editOrNewMetadata()
                .withName(routeName)
                .withNamespace(namespace)
                .addToAnnotations(exposeSpec.getAnnotations() != null ? exposeSpec.getAnnotations() : Collections.emptyMap())
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                .addToAnnotations(AnnotationKeys.SERVICE_NAME, serviceName)
                .addToLabels(LabelKeys.INFRA_TYPE, addressSpace.getType())
                .addToLabels(LabelKeys.INFRA_UUID, infraUuid)
                .endMetadata()
                .editOrNewSpec()
                .withHost(endpointSpec.getExposeSpec().flatMap(ExposeSpec::getRouteHost).orElse(""))
                .withNewTo()
                .withName(serviceName)
                .withKind("Service")
                .endTo()
                .withNewPort()
                .editOrNewTargetPort()
                .withStrVal(exposeSpec.getRouteServicePort())
                .endTargetPort()
                .endPort()
                .endSpec();


        if (endpointSpec.getCertSpec().isPresent()) {
            ExposeSpec.TlsTermination tlsTermination = exposeSpec.getRouteTlsTermination();
            CertSpec certSpec = endpointSpec.getCertSpec().get();

            if (tlsTermination.equals(ExposeSpec.TlsTermination.passthrough)) {
                route.editOrNewSpec()
                    .withNewTls()
                    .withTermination("passthrough")
                    .endTls()
                    .endSpec();
            } else if (tlsTermination.equals(ExposeSpec.TlsTermination.reencrypt)) {
                if ("selfsigned".equals(certSpec.getProvider())) {
                    String caSecretName = KubeUtil.getAddressSpaceExternalCaSecretName(addressSpace);
                    Secret secret = client.secrets().inNamespace(namespace).withName(caSecretName).get();
                    if (secret != null) {
                        String consoleCa = new String(Base64.getDecoder().decode(secret.getData().get("tls.crt")), StandardCharsets.UTF_8);
                        route.editOrNewSpec()
                                .withNewTls()
                                .withTermination("reencrypt")
                                .withDestinationCACertificate(consoleCa)
                                .endTls()
                                .endSpec();
                    } else {
                        log.info("Ca secret {} for endpoint {} does not yet exist, skipping route creation for now", caSecretName, endpointSpec);
                        return null; // Skip this route until secret is available
                    }
                } else {
                    route.editOrNewSpec()
                            .withNewTls()
                            .withTermination("reencrypt")
                            .endTls()
                            .endSpec();
                }
            }
        }
        log.info("Creating route {} for endpoint {}", routeName, endpointSpec.getName());
        return client.routes().inNamespace(namespace).create(route.build());
    }

    private Service ensureExternalServiceExists(AddressSpace addressSpace, EndpointSpec endpointSpec, ExposeSpec exposeSpec) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        String serviceName = endpointSpec.getName() + "-" + infraUuid + "-external";

        Service existingService = client.services().inNamespace(namespace).withName(serviceName).get();
        if (existingService != null) {
            return existingService;
        }

        Service service = client.services().inNamespace(namespace).withName(KubeUtil.getAddressSpaceServiceName(endpointSpec.getService(), addressSpace)).get();
        if (service == null) {
            return null;
        }


        List<ServicePort> servicePorts = new ArrayList<>();
        for (ServicePort port : service.getSpec().getPorts()) {
            for (String portName : exposeSpec.getLoadBalancerPorts()) {
                if (port.getName().equals(portName)) {
                    servicePorts.add(port);
                }
            }
        }
        if (servicePorts.isEmpty()) {
            return null;
        }

        ServiceBuilder svc = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(serviceName)
                .withNamespace(namespace)
                .addToAnnotations(exposeSpec.getAnnotations())
                .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                .addToAnnotations(AnnotationKeys.SERVICE_NAME, KubeUtil.getAddressSpaceServiceName(endpointSpec.getService(), addressSpace))
                .addToLabels(LabelKeys.INFRA_TYPE, addressSpace.getType())
                .addToLabels(LabelKeys.INFRA_UUID, infraUuid)
                .endMetadata()
                .editOrNewSpec()
                .withPorts(servicePorts)
                .withSelector(service.getSpec().getSelector())
                .withLoadBalancerSourceRanges(exposeSpec.getLoadBalancerSourceRanges() != null ? exposeSpec.getLoadBalancerSourceRanges() : Collections.emptyList())
                .withType("LoadBalancer")
                .endSpec();

        log.info("Creating loadbalancer service {} for endpoint {}", serviceName, endpointSpec.getName());
        return client.services().inNamespace(namespace).create(svc.build());
    }

    @Override
    public String toString() {
        return "EndpointController";
    }
}
