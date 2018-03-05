/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class WildcardCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(WildcardCertProvider.class);
    private final KubernetesClient client;
    private final CertSpec certSpec;
    private final String wildcardSecretName;

    public WildcardCertProvider(KubernetesClient client, CertSpec certSpec, String wildcardSecretName) {
        this.client = client;
        this.certSpec = certSpec;
        this.wildcardSecretName = wildcardSecretName;
    }

    @Override
    public Secret provideCert(AddressSpace addressSpace, Endpoint endpoint) {
        Secret secret = client.secrets().inNamespace(addressSpace.getNamespace()).withName(certSpec.getSecretName()).get();
        if (secret == null) {
            Secret wildcardSecret = null;
            if (wildcardSecretName != null) {
                wildcardSecret = client .secrets().withName(wildcardSecretName).get();
            }
            if (wildcardSecret == null) {
                String message = String.format("Requested 'wildcard' certificate provider but no secret '%s' found", wildcardSecretName);
                throw new IllegalStateException(message);
            }
            log.info("Copying wildcard certificate for {}", endpoint);

            Map<String, String> data = new LinkedHashMap<>();
            data.put("tls.key", wildcardSecret.getData().get("tls.key"));
            data.put("tls.crt", wildcardSecret.getData().get("tls.crt"));

            secret = client.secrets().inNamespace(addressSpace.getNamespace()).createNew()
                    .editOrNewMetadata()
                    .withName(certSpec.getSecretName())
                    .endMetadata()
                    .withType("kubernetes.io/tls")
                    .addToData(data)
                    .done();
        }
        return secret;
    }
}
