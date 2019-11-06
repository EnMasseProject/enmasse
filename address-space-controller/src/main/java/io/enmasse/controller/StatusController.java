/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.user.api.UserApi;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.enmasse.controller.InfraConfigs.parseCurrentInfraConfig;

import java.util.*;
import java.util.stream.Collectors;

public class StatusController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(StatusController.class.getName());
    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final InfraResourceFactory infraResourceFactory;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final UserApi userApi;

    public StatusController(Kubernetes kubernetes, SchemaProvider schemaProvider, InfraResourceFactory infraResourceFactory, AuthenticationServiceRegistry authenticationServiceRegistry, UserApi userApi) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.infraResourceFactory = infraResourceFactory;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
        this.userApi = userApi;
    }

    @Override
    public AddressSpace reconcile(AddressSpace addressSpace) {
        if (addressSpace.getStatus().isReady()) {
            checkComponentsReady(addressSpace);
            checkAuthServiceReady(addressSpace);
            checkExposedEndpoints(addressSpace);
        }

        if (addressSpace.getStatus().isReady()) {
            if (addressSpace.getSpec().getPlan().equals(addressSpace.getAnnotation(AnnotationKeys.APPLIED_PLAN))) {
                addressSpace.getStatus().setPhase(Phase.Active);
            }
        } else {
            if (Phase.Active.equals(addressSpace.getStatus().getPhase())) {
                addressSpace.getStatus().setPhase(Phase.Failed);
            }
        }
        return addressSpace;
    }

    private void checkExposedEndpoints(AddressSpace addressSpace) {
        Map<String, EndpointSpec> exposedEndpoints = new HashMap<>();
        for (EndpointSpec endpointSpec : addressSpace.getSpec().getEndpoints()) {
            if (endpointSpec.getExpose() != null && endpointSpec.getExpose().getType().equals(ExposeType.route)) {
                exposedEndpoints.put(endpointSpec.getName(), endpointSpec);
            }
        }

        for (EndpointStatus endpointStatus : addressSpace.getStatus().getEndpointStatuses()) {
            if (exposedEndpoints.containsKey(endpointStatus.getName())) {
                if (endpointStatus.getExternalHost() == null) {
                    String msg = String.format("Endpoint '%s' is not yet exposed", endpointStatus.getName());
                    addressSpace.getStatus().setReady(false);
                    addressSpace.getStatus().appendMessage(msg);
                }
            }
        }
    }

    private InfraConfig getInfraConfig(AddressSpace addressSpace) {
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        return addressSpaceResolver.getInfraConfig(addressSpace.getSpec().getType(), addressSpace.getSpec().getPlan());
    }

    private void checkComponentsReady(AddressSpace addressSpace) {
        try {
            InfraConfig infraConfig = Optional.ofNullable(parseCurrentInfraConfig(addressSpace)).orElseGet(() -> getInfraConfig(addressSpace));
            List<HasMetadata> requiredResources = infraResourceFactory.createInfraResources(addressSpace, infraConfig);

            checkDeploymentsReady(addressSpace, requiredResources);
            checkStatefulSetsReady(addressSpace, requiredResources);
        } catch (Exception e) {
            String msg = String.format("Error checking for ready components: %s", e.getMessage());
            log.warn(msg, e);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage(msg);
        }
    }

    private void checkStatefulSetsReady(AddressSpace addressSpace, List<HasMetadata> requiredResources) {
        Set<String> readyStatefulSets = kubernetes.getReadyStatefulSets(addressSpace).stream()
                .map(statefulSet -> statefulSet.getMetadata().getName())
                .collect(Collectors.toSet());


        Set<String> requiredStatefulSets = requiredResources.stream()
                .filter(KubernetesHelper::isStatefulSet)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        boolean isReady = readyStatefulSets.containsAll(requiredStatefulSets);
        if (!isReady) {
            Set<String> missing = new HashSet<>(requiredStatefulSets);
            missing.removeAll(readyStatefulSets);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("The following stateful sets are not ready: " + missing);
        }
    }

    private void checkDeploymentsReady(AddressSpace addressSpace, List<HasMetadata> requiredResources) {
        Set<String> readyDeployments = kubernetes.getReadyDeployments(addressSpace).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());


        Set<String> requiredDeployments = requiredResources.stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        boolean isReady = readyDeployments.containsAll(requiredDeployments);
        if (!isReady) {
            Set<String> missing = new HashSet<>(requiredDeployments);
            missing.removeAll(readyDeployments);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("The following deployments are not ready: " + missing);
        }
    }

    private void checkAuthServiceReady(AddressSpace addressSpace) {
        AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService()).orElse(null);
        if (authenticationService != null) {
            String realm = authenticationService.getSpec().getRealm();
            if (realm == null) {
                realm = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
            }
            try {
                boolean isReady = userApi.realmExists(authenticationService, realm);
                if (!isReady) {
                    addressSpace.getStatus().setReady(false);
                    addressSpace.getStatus().appendMessage("Authentication service is not configured with realm " + addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
                }
            } catch (Exception e) {
                String msg = String.format("Error checking authentication service status: %s", e.getMessage());
                log.warn(msg);
                addressSpace.getStatus().setReady(false);
                addressSpace.getStatus().appendMessage(msg);
            }
        }
    }

    @Override
    public String toString() {
        return "StatusController";
    }
}
