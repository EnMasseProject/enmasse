/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CertProviderSpec;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfsignedCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(SelfsignedCertProvider.class);
    private final OpenShiftClient client;
    private final CertProviderSpec certProviderSpec;
    private final CertManager certManager;

    public SelfsignedCertProvider(OpenShiftClient client, CertProviderSpec certProviderSpec, CertManager certManager) {
        this.client = client;
        this.certProviderSpec = certProviderSpec;
        this.certManager = certManager;
    }

    @Override
    public Secret provideCert(AddressSpace addressSpace, Endpoint endpoint) {
        Secret secret = client.secrets().inNamespace(addressSpace.getNamespace()).withName(certProviderSpec.getSecretName()).get();
        if (secret == null) {
            log.info("Creating self-signed certificates for {}", endpoint);
            secret = certManager.createSelfSignedCertSecret(addressSpace.getNamespace(), certProviderSpec.getSecretName());
        }
        return secret;
    }
}
