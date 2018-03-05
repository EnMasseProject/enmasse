/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.util.List;
import java.util.stream.Collectors;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.KubeUtil;
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

    public void issueExternalCertificates(AddressSpace addressSpace) throws Exception {
        List<Endpoint> endpoints = addressSpace.getEndpoints();
        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                if (endpoint.getCertSpec().isPresent()) {
                    try {
                        CertProvider certProvider = certProviderFactory.createProvider(endpoint.getCertSpec().get());
                        Secret secret = certProvider.provideCert(addressSpace, endpoint);
                        certManager.grantServiceAccountAccess(secret, "default", addressSpace.getNamespace());
                    } catch (Exception e) {
                        log.warn("Error providing certificate for {}: {}", endpoint, e.getMessage(), e);
                    }
                }
            }
        }
    }


    public Secret issueAddressSpaceCert(final AddressSpace addressSpace)
    {
        try {
            final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace);
            Secret secret = certManager.getCertSecret(addressSpace.getNamespace(), addressSpaceCaSecretName);
            if (secret == null) {
                secret = certManager.createSelfSignedCertSecret(addressSpace.getNamespace(), addressSpaceCaSecretName);
                //put crt into address space
                eventLogger.log(CertCreated, "Created address space CA", Normal, ControllerKind.AddressSpace, addressSpace.getName());
            }
            return secret;
        } catch (Exception e) {
            log.warn("Error issuing addressspace ca certificate", e);
            eventLogger.log(CertCreateFailed, "Error creating certificate: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getName());
            return null;
        }
    }

    public void issueComponentCertificates(AddressSpace addressSpace, Secret addressSpaceCaSecret) {
        try {
            List<Cert> certs = certManager.listComponents(addressSpace.getNamespace()).stream()
                    .filter(component -> !certManager.certExists(component))
                    .map(certManager::createCsr)
                    .map(request -> certManager.signCsr(request, addressSpaceCaSecret))
                    .map(cert -> {
                        certManager.createSecret(cert, addressSpaceCaSecret);
                        return cert; })
                    .collect(Collectors.toList());

            if (!certs.isEmpty()) {
                eventLogger.log(CertCreated, "Created component certificates", Normal, ControllerKind.AddressSpace, addressSpace.getName());
            }
        } catch (Exception e) {
            log.warn("Error issuing component certificates", e);
            eventLogger.log(CertCreateFailed, "Error creating component certificates: " + e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getName());
        }
    }

    public String getDefaultCertProvider() {
        return certProviderFactory.getDefaultProviderName();
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
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
