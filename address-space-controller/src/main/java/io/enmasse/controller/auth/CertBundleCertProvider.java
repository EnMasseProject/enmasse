/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        Secret secret = client.secrets().inNamespace(namespace).withName(endpointInfo.getCertSpec().getSecretName()).get();
        if (secret == null) {
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

            data.put("tls.key", tlsKey);
            data.put("tls.crt", tlsCert);
            log.info("Creating cert secret with certBundle input");
            client.secrets().inNamespace(namespace).withName(endpointInfo.getCertSpec().getSecretName()).createNew()
                    .editOrNewMetadata()
                    .withName(endpointInfo.getCertSpec().getSecretName())
                    .addToLabels(LabelKeys.INFRA_UUID, addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID))
                    .addToLabels(LabelKeys.INFRA_TYPE, addressSpace.getType())
                    .addToLabels("app", "enmasse")
                    .endMetadata()
                    .withType("kubernetes.io/tls")
                    .withData(data)
                    .done();
        }
    }
}
