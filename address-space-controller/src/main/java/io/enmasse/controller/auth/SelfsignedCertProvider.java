/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SelfsignedCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(SelfsignedCertProvider.class);
    private final OpenShiftClient client;
    private final CertSpec certSpec;
    private final CertManager certManager;
    private final String namespace;

    public SelfsignedCertProvider(OpenShiftClient client, CertSpec certSpec, CertManager certManager) {
        this.client = client;
        this.certSpec = certSpec;
        this.certManager = certManager;
        this.namespace = client.getNamespace();
    }

    private Secret issueAddressSpaceCert(final AddressSpace addressSpace, Map<String, String> labels) {
        try {
            final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceExternalCaSecretName(addressSpace);
            Secret secret = certManager.getCertSecret(addressSpaceCaSecretName);
            if (secret == null) {
                secret = certManager.createSelfSignedCertSecret(addressSpaceCaSecretName, labels);
                log.info("Created CA secret for {}", addressSpace.getName());
            }
            return secret;
        } catch (Exception e) {
            log.warn("Error issuing self-signed external route ca certificate", e);
            return null;
        }
    }

    @Override
    public Secret provideCert(AddressSpace addressSpace, String cn, Collection<String> sans) {
        Secret secret = client.secrets().inNamespace(namespace).withName(certSpec.getSecretName()).get();
        if (secret == null) {
            log.info("Creating self-signed certificates for {}", cn);

            String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
            Map<String, String> labels = new HashMap<>();
            labels.put(LabelKeys.INFRA_TYPE, addressSpace.getType());
            labels.put(LabelKeys.INFRA_UUID, infraUuid);
            if (cn != null) {
                Secret ca = issueAddressSpaceCert(addressSpace, labels);
                CertComponent component = new CertComponent(cn, namespace, certSpec.getSecretName());
                CertSigningRequest csr = certManager.createCsr(component);
                Cert cert = certManager.signCsr(csr, ca, sans);

                secret = certManager.createSecret(cert, ca, labels);
            } else {
                secret = certManager.createSelfSignedCertSecret(certSpec.getSecretName(), labels);
            }
        }
        return secret;
    }
}
