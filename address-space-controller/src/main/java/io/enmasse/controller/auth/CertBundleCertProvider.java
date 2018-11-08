/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CertBundleCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(CertBundleCertProvider.class);
    private final OpenShiftClient client;
    private final String namespace;

    public CertBundleCertProvider(OpenShiftClient client) {
        this.client = client;
        this.namespace = client.getNamespace();
    }

    @Override
    public void provideCert(AddressSpace addressSpace, EndpointInfo endpointInfo) {
        Map<String, String> data = new HashMap<>();
        String tlsKey = endpointInfo.getCertSpec().getTlsKey();
        String tlsCert = endpointInfo.getCertSpec().getTlsCert();
        if (tlsKey == null) {
            log.warn("tlsKey not present, not providing cert for {}", endpointInfo.getServiceName());
            return;
        }

        if (tlsCert == null) {
            log.warn("tlsCert not present, not providing cert for {}", endpointInfo.getServiceName());
            return;
        }

        boolean isPemFormat = tlsCert.startsWith("-----BEGIN");
        if (isPemFormat) {
            Base64.Encoder b64enc = Base64.getEncoder();
            String tlsKeyB64 = b64enc.encodeToString(tlsKey.getBytes(StandardCharsets.UTF_8));
            String tlsCertB64 = b64enc.encodeToString(tlsCert.getBytes(StandardCharsets.UTF_8));
            data.put("tls.key", tlsKeyB64);
            data.put("tls.crt", tlsCertB64);
        } else { // Assume already base64 encoded
            data.put("tls.key", tlsKey);
            data.put("tls.crt", tlsCert);
        }

        Secret secret = new SecretBuilder()
                .editOrNewMetadata()
                .withName(endpointInfo.getCertSpec().getSecretName())
                .withNamespace(namespace)
                .addToLabels(LabelKeys.INFRA_UUID, addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID))
                .addToLabels(LabelKeys.INFRA_TYPE, addressSpace.getType())
                .addToLabels("app", "enmasse")
                .endMetadata()
                .withType("kubernetes.io/tls")
                .withData(data)
                .build();

        Secret existing = client.secrets().inNamespace(namespace).withName(endpointInfo.getCertSpec().getSecretName()).get();
        if (existing == null) {
            log.info("Creating cert secret {} with certBundle input", secret.getMetadata().getName());
            client.secrets().inNamespace(namespace).createOrReplace(secret);
        } else if (!data.equals(existing.getData())) {
            log.info("Replacing cert secret {} with certBundle input", secret.getMetadata().getName());
            client.secrets().inNamespace(namespace).withName(endpointInfo.getCertSpec().getSecretName()).patch(secret);
        }
    }
}
