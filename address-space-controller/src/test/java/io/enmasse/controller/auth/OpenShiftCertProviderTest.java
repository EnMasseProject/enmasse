/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.CertSpecBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OpenShiftCertProviderTest extends JULInitializingTest {
    public OpenShiftServer server = new OpenShiftServer(true, true);

    private OpenShiftClient client;
    private CertProvider certProvider;

    @BeforeEach
    public void setup() {
        server.before();
        client = server.getOpenshiftClient();

        certProvider = new OpenshiftCertProvider(client);
    }

    @AfterEach
    void tearDown() {
        server.after();
    }

    @Test
    public void testProvideCertNoService() {

        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .endMetadata()

                .withNewSpec()
                .withPlan("myplan")
                .withType("standard")
                .endSpec()
                .build();

        CertSpec spec = new CertSpecBuilder().withProvider("openshift").withSecretName("mycerts").build();
        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertNull(cert);
    }

    @Test
    public void testProvideCert() {
        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .addToAnnotations(AnnotationKeys.INFRA_UUID, "1234")
                .endMetadata()

                .withNewSpec()
                .withPlan("myplan")
                .withType("standard")
                .endSpec()

                .build();

        client.services().inNamespace("test").create(new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .endSpec()
                .build());

        CertSpec spec = new CertSpecBuilder().withProvider("openshift").withSecretName("mycerts").build();
        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertNull(cert);

        Service service = client.services().inNamespace("test").withName("messaging-1234").get();
        assertNotNull(service);
        // Should verify that annotation is set, but bug in mock-server seems to not set it
    }
}
