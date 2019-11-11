/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.util.*;
import java.util.stream.Collectors;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.CertProviderFactory;
import io.enmasse.controller.Controller;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.api.model.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.enmasse.controller.common.ControllerReason.CertCreateFailed;
import static io.enmasse.controller.common.ControllerReason.CertCreated;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * Manages certificates issuing, revoking etc. for EnMasse services
 */
public class AuthController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class.getName());

    private final CertManager certManager;
    private final EventLogger eventLogger;
    private final CertProviderFactory certProviderFactory;

    public AuthController(CertManager certManager,
                          EventLogger eventLogger,
                          CertProviderFactory certProviderFactory) {
        this.certManager = certManager;
        this.eventLogger = eventLogger;
        this.certProviderFactory = certProviderFactory;
    }

    public void issueExternalCertificates(AddressSpace addressSpace) {
        List<EndpointSpec> endpoints = addressSpace.getSpec().getEndpoints();
        if (endpoints != null) {
            Map<String, EndpointSpec> endpointSpecMap = new HashMap<>();
            Map<String, EndpointInfo> endpointInfoMap = new HashMap<>();

            for (EndpointSpec endpoint : endpoints) {
                endpointSpecMap.put(endpoint.getName(), endpoint);
                if (endpoint.getCert() != null) {
                    EndpointInfo info = endpointInfoMap.get(endpoint.getService());
                    if (info == null) {
                        info = new EndpointInfo(endpoint.getService(), endpoint.getCert());
                        endpointInfoMap.put(endpoint.getService(), info);
                    }
                }
            }

            for (EndpointStatus status : addressSpace.getStatus().getEndpointStatuses()) {
                EndpointSpec spec = endpointSpecMap.get(status.getName());
                EndpointInfo info = endpointInfoMap.get(spec.getService());
                if (info != null) {
                    info.addHost(status.getServiceHost());
                    if (status.getExternalHost() != null && !status.getExternalHost().isEmpty()) {
                        info.addHost(status.getExternalHost());
                    }
                }
            }

            for (EndpointInfo info : endpointInfoMap.values()) {
                try {
                    CertProvider certProvider = certProviderFactory.createProvider(info.getCertSpec().getProvider());
                    certProvider.provideCert(addressSpace, info);
                } catch (Exception e) {
                    log.warn("Error providing certificate for service {} hosts {}: {}", info.getServiceName(), info.getHosts(), e.getMessage(), e);
                }
            }
        }
    }


    public Secret issueAddressSpaceCert(final AddressSpace addressSpace) {
        try {
            final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace);
            Secret secret = certManager.getCertSecret(addressSpaceCaSecretName);
            if (secret == null) {
                String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
                Map<String, String> labels = new HashMap<>();
                labels.put(LabelKeys.INFRA_UUID, infraUuid);
                labels.put(LabelKeys.INFRA_TYPE, addressSpace.getSpec().getType());
                secret = certManager.createSelfSignedCertSecret(addressSpaceCaSecretName, labels);
                //put crt into address space
                eventLogger.log(CertCreated, "Created address space CA", Normal, ControllerKind.AddressSpace, addressSpace.getMetadata().getName());
            }
            return secret;
        } catch (Exception e) {
            log.warn("Error issuing addressspace ca certificate", e);
            eventLogger.log(CertCreateFailed, "Error creating certificate: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getMetadata().getName());
            return null;
        }
    }

    public void issueComponentCertificates(AddressSpace addressSpace, Secret addressSpaceCaSecret) {
        try {
            Map<String, String> labels = new HashMap<>();
            String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
            labels.put(LabelKeys.INFRA_UUID, infraUuid);
            labels.put(LabelKeys.INFRA_TYPE, addressSpace.getSpec().getType());
            List<Cert> certs = certManager.listComponents(infraUuid).stream()
                    .filter(component -> !certManager.certExists(component))
                    .map(certManager::createCsr)
                    .map(request -> certManager.signCsr(request, addressSpaceCaSecret, Collections.emptySet()))
                    .map(cert -> {
                        certManager.createSecret(cert, addressSpaceCaSecret, labels);
                        return cert; })
                    .collect(Collectors.toList());

            if (!certs.isEmpty()) {
                eventLogger.log(CertCreated, "Created component certificates", Normal, ControllerKind.AddressSpace, addressSpace.getMetadata().getName());
            }
        } catch (Exception e) {
            log.warn("Error issuing component certificates", e);
            eventLogger.log(CertCreateFailed, "Error creating component certificates: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getMetadata().getName());
        }
    }

    public String getDefaultCertProvider() {
        return certProviderFactory.getDefaultProviderName();
    }

    @Override
    public AddressSpace reconcile(AddressSpace addressSpace) throws Exception {

        if ( Controller.isDeleted(addressSpace)) {
            return addressSpace;
        }

        Secret addressSpaceCa = issueAddressSpaceCert(addressSpace);
        if (addressSpaceCa != null) {
            issueComponentCertificates(addressSpace, addressSpaceCa);
        }
        issueExternalCertificates(addressSpace);
        return addressSpace;
    }

    @Override
    public String toString() {
        return "AuthController";
    }
}
