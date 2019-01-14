/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.CertSpecBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class WildcardCertProviderTest {
    public KubernetesServer server = new KubernetesServer(true, true);

    private KubernetesClient client;
    private CertProvider certProvider;

    @BeforeEach
    public void setup() {
        server.before();
        client = server.getClient();
        String wildcardCert = "wildcardcert";

        certProvider = new WildcardCertProvider(client, wildcardCert);
    }

    @AfterEach
    void tearDown() {
        server.after();
    }

    @Test
    public void testUnknownWildcardSecret() {

        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .endMetadata()

                .withNewSpec()
                .withType("standard")
                .withPlan("myplan")
                .endSpec()
                .build();
        CertSpec spec = new CertSpecBuilder().withProvider("wildcard").withSecretName("mycerts").build();

        assertThrows(IllegalStateException.class, () -> certProvider.provideCert(space, new EndpointInfo("messaging", spec)));
    }

    @Test
    public void testProvideCert() {

        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .endMetadata()

                .withNewSpec()
                .withPlan("myplan")
                .withType("standard")
                .endSpec()
                .build();

        client.secrets().create(new SecretBuilder()
                .editOrNewMetadata()
                .withName("wildcardcert")
                .endMetadata()
                .addToData("tls.key", "mykey")
                .addToData("tls.crt", "myvalue")
                .build());

        CertSpec spec = new CertSpecBuilder().withProvider("wildcard").withSecretName("mycerts").build();
        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertThat(cert.getData().get("tls.key"), is("mykey"));
        assertThat(cert.getData().get("tls.crt"), is("myvalue"));
    }
}
