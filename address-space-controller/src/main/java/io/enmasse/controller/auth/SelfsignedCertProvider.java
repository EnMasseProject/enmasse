/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfsignedCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(SelfsignedCertProvider.class);
    private final KubernetesClient client;
    private final CertManager certManager;
    private final String namespace;

    public SelfsignedCertProvider(KubernetesClient client, CertManager certManager) {
        this.client = client;
        this.certManager = certManager;
        this.namespace = client.getNamespace();
    }

    private Secret issueAddressSpaceCert(final AddressSpace addressSpace, Map<String, String> labels) {
        try {
            final String addressSpaceCaSecretName = KubeUtil.getAddressSpaceExternalCaSecretName(addressSpace);
            Secret secret = certManager.getCertSecret(addressSpaceCaSecretName);
            if (secret == null) {
                secret = certManager.createSelfSignedCertSecret(addressSpaceCaSecretName, labels);
                log.info("Created CA secret for {}", addressSpace.getMetadata().getName());
            }
            return secret;
        } catch (Exception e) {
            log.warn("Error issuing self-signed external route ca certificate", e);
            return null;
        }
    }

    @Override
    public void provideCert(AddressSpace addressSpace, EndpointInfo info) {
        Secret secret = client.secrets().inNamespace(namespace).withName(info.getCertSpec().getSecretName()).get();
        if (secret == null) {
            List<String> hosts = info.getHosts();
            String cn = null;
            if (!hosts.isEmpty()) {
                cn = hosts.iterator().next();
            }

            log.info("Creating self-signed certificates for {}", cn);
            String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
            Map<String, String> labels = new HashMap<>();
            labels.put(LabelKeys.INFRA_TYPE, addressSpace.getSpec().getType());
            labels.put(LabelKeys.INFRA_UUID, infraUuid);
            if (cn != null) {
                Secret ca = issueAddressSpaceCert(addressSpace, labels);
                if (ca != null) {
                    CertComponent component = new CertComponent(cn, namespace, info.getCertSpec().getSecretName());
                    CertSigningRequest csr = certManager.createCsr(component);
                    Cert cert = certManager.signCsr(csr, ca, hosts);

                    certManager.createSecret(cert, ca, labels);
                }
            } else {
                certManager.createSelfSignedCertSecret(info.getCertSpec().getSecretName(), labels);
            }
        }
    }
}
