/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SelfsignedCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(SelfsignedCertProvider.class);
    private final OpenShiftClient client;
    private final CertSpec certSpec;
    private final CertManager certManager;

    public SelfsignedCertProvider(OpenShiftClient client, CertSpec certSpec, CertManager certManager) {
        this.client = client;
        this.certSpec = certSpec;
        this.certManager = certManager;
    }

    private Secret issueAddressSpaceCert(final AddressSpace addressSpace) {
        try {
            final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceExternalCaSecretName(addressSpace);
            Secret secret = certManager.getCertSecret(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE), addressSpaceCaSecretName);
            if (secret == null) {
                secret = certManager.createSelfSignedCertSecret(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE), addressSpaceCaSecretName);
            }
            return secret;
        } catch (Exception e) {
            log.warn("Error issuing self-signed external route ca certificate", e);
            return null;
        }
    }

    @Override
    public Secret provideCert(AddressSpace addressSpace, String cn, Set<String> sans) {
        String namespace = addressSpace.getAnnotation(AnnotationKeys.NAMESPACE);
        Secret secret = client.secrets().inNamespace(namespace).withName(certSpec.getSecretName()).get();
        if (secret == null) {
            log.info("Creating self-signed certificates for {}", cn);
            if (cn != null) {
                Secret ca = issueAddressSpaceCert(addressSpace);
                CertComponent component = new CertComponent(cn, namespace, certSpec.getSecretName());
                CertSigningRequest csr = certManager.createCsr(component);
                Cert cert = certManager.signCsr(csr, ca, sans);
                secret = certManager.createSecret(cert, ca);
            } else {
                secret = certManager.createSelfSignedCertSecret(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE), certSpec.getSecretName());
            }
        }
        return secret;
    }
}
