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
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.enmasse.controller.common.ControllerReason.CertCreateFailed;
import static io.enmasse.controller.common.ControllerReason.CertCreated;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * Manages certificates issuing, revoking etc. for EnMasse services
 */
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class.getName());

    private final CertManager certManager;
    private final EventLogger eventLogger;

    public AuthController(CertManager certManager,
                          EventLogger eventLogger) {
        this.certManager = certManager;
        this.eventLogger = eventLogger;
    }

    public void issueExternalCertificates(AddressSpace addressSpace) throws Exception {
        List<Endpoint> endpoints = addressSpace.getEndpoints();
        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                if (endpoint.getCertProvider().isPresent()) {
                    String secretName = endpoint.getCertProvider().get().getSecretName();
                    certManager.issueRouteCert(secretName, addressSpace.getNamespace());
                }
            }
        }
    }


    public void issueAddressSpaceCert(final AddressSpace addressSpace)
    {
        try {
            final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace.getNamespace());
            if(!certManager.certExists(addressSpaceCaSecretName)) {
                certManager.createSelfSignedCertSecret(addressSpaceCaSecretName);
                //put crt into address space
                log.info("Issued addressspace ca certificates for {}", addressSpace.getName());
                eventLogger.log(CertCreated, "Created address space CA", Normal, ControllerKind.AddressSpace, addressSpace.getName());
            }
        } catch (Exception e) {
            log.warn("Error issuing addressspace ca certificate", e);
            eventLogger.log(CertCreateFailed, "Error creating certificate",Warning, ControllerKind.AddressSpace, addressSpace.getName());
        }
    }

    public void issueComponentCertificates(AddressSpace addressSpace) {
        try {
            final String caSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace.getNamespace());
            List<Cert> certs = certManager.listComponents(addressSpace.getNamespace()).stream()
                    .filter(component -> !certManager.certExists(component))
                    .map(certManager::createCsr)
                    .map(request -> certManager.signCsr(request,
                                                        caSecretName))
                    .map(cert -> {
                        certManager.createSecret(cert, caSecretName);
                        return cert; })
                    .collect(Collectors.toList());

            if (!certs.isEmpty()) {
                log.info("Issued component certificates: {}", certs);
                eventLogger.log(CertCreated, "Created component certificates", Normal, ControllerKind.AddressSpace, addressSpace.getName());
            }
        } catch (Exception e) {
            log.warn("Error issuing component certificates", e);
            eventLogger.log(CertCreateFailed, "Error creating component certificates", Warning, ControllerKind.AddressSpace, addressSpace.getName());
        }
    }
}
