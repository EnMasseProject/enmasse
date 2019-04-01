/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenshiftCertProvider implements CertProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftCertProvider.class);
    private final KubernetesClient client;
    private final String namespace;

    public OpenshiftCertProvider(KubernetesClient client) {
        this.client = client;
        this.namespace = client.getNamespace();
    }

    @Override
    public void provideCert(AddressSpace addressSpace, EndpointInfo endpointInfo) {
        Secret secret = client.secrets().inNamespace(namespace).withName(endpointInfo.getCertSpec().getSecretName()).get();
        if (secret == null) {
            String serviceName = KubeUtil.getAddressSpaceServiceName(endpointInfo.getServiceName(), addressSpace);
            Service service = client.services().withName(serviceName).get();
            if (service != null && (service.getMetadata().getAnnotations() == null || service.getMetadata().getAnnotations().get(AnnotationKeys.OPENSHIFT_SERVING_CERT_SECRET_NAME) == null)) {
                log.info("Adding service annotation to generate OpenShift cert");
                client.services().withName(serviceName).edit()
                        .editMetadata()
                        .addToAnnotations(AnnotationKeys.OPENSHIFT_SERVING_CERT_SECRET_NAME, endpointInfo.getCertSpec().getSecretName())
                        .endMetadata()
                        .done();
            }
        }
    }
}
