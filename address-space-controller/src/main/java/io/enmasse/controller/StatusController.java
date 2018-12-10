/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.user.api.UserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StatusController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(StatusController.class.getName());
    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final InfraResourceFactory infraResourceFactory;
    private final UserApi userApi;

    public StatusController(Kubernetes kubernetes, SchemaProvider schemaProvider, InfraResourceFactory infraResourceFactory, UserApi userApi) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.infraResourceFactory = infraResourceFactory;
        this.userApi = userApi;
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
        checkDeploymentsReady(addressSpace);
        checkAuthServiceReady(addressSpace);
        return addressSpace;
    }

    private InfraConfig getInfraConfig(AddressSpace addressSpace) {
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        return addressSpaceResolver.getInfraConfig(addressSpace.getType(), addressSpace.getPlan());
    }

    private InfraConfig parseCurrentInfraConfig(AddressSpace addressSpace) throws IOException {
        if (addressSpace.getAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG) == null) {
            return null;
        }
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        AddressSpaceType type = addressSpaceResolver.getType(addressSpace.getType());
        return type.getInfraConfigDeserializer().fromJson(addressSpace.getAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG));
    }

    private void checkDeploymentsReady(AddressSpace addressSpace) {
        try {
            Set<String> readyDeployments = kubernetes.getReadyDeployments().stream()
                    .map(deployment -> deployment.getMetadata().getName())
                    .collect(Collectors.toSet());

            InfraConfig infraConfig = Optional.ofNullable(parseCurrentInfraConfig(addressSpace)).orElseGet(() -> getInfraConfig(addressSpace));
            Set<String> requiredDeployments = infraResourceFactory.createInfraResources(addressSpace, infraConfig).stream()
                    .filter(KubernetesHelper::isDeployment)
                    .map(item -> item.getMetadata().getName())
                    .collect(Collectors.toSet());

            boolean isReady = readyDeployments.containsAll(requiredDeployments);
            if (!isReady) {
                Set<String> missing = new HashSet<>(requiredDeployments);
                missing.removeAll(readyDeployments);
                addressSpace.getStatus().setReady(false);
                addressSpace.getStatus().appendMessage("Following deployments and statefulsets are not ready: " + missing);
            }
        } catch (Exception e) {
            String msg = String.format("Error checking for ready deployments: %s", e.getMessage());
            log.warn(msg);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage(msg);
        }
    }

    private void checkAuthServiceReady(AddressSpace addressSpace) {
        if (AuthenticationServiceType.STANDARD.equals(addressSpace.getAuthenticationService().getType())) {
            try {
                boolean isReady = userApi.realmExists(addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
                if (!isReady) {
                    addressSpace.getStatus().setReady(false);
                    addressSpace.getStatus().appendMessage("Standard authentication service is not configured with realm " + addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
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
