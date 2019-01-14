/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.CertSpecBuilder;
import io.enmasse.model.validation.DefaultValidator;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import javax.validation.ValidationException;

public class CertBundleCertProviderTest {

    public OpenShiftServer server = new OpenShiftServer(true, true);

    private OpenShiftClient client;
    private CertProvider certProvider;

    @AfterEach
    void tearDown() {
        server.after();
    }

    @BeforeEach
    public void setup() {
        server.before();
        client = server.getOpenshiftClient();

        certProvider = new CertBundleCertProvider(client);
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

        CertSpec spec = new CertSpecBuilder()
                .withProvider("certBundle")
                .withSecretName("mycerts")
                .build();

        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertNull(cert);
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

        CertSpec spec = new CertSpecBuilder()
                .withProvider("certBundle")
                .withSecretName("mycerts")
                .withTlsKey("aGVsbG8=")
                .withTlsCert("d29ybGQ=")
                .build();

        DefaultValidator.validate(space);

        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertNotNull(cert);
        assertThat(cert.getData().get("tls.key"), is(spec.getTlsKey()));
        assertThat(cert.getData().get("tls.crt"), is(spec.getTlsCert()));
    }

    @Test
    public void testValidateBadKey() {
        assertThrows(ValidationException.class, () -> DefaultValidator.validate(
                new CertSpecBuilder()
                        .withProvider("certBundle")
                        .withSecretName("mycerts")
                        .withTlsKey("/%^$lkg")
                        .withTlsCert("d29ybGQ=")
                        .build()));
    }

    @Test
    public void testValidateBadCert() {
        assertThrows(ValidationException.class, () -> DefaultValidator.validate(
                new CertSpecBuilder()
                        .withProvider("certBundle")
                        .withSecretName("mycerts")
                        .withTlsKey("d29ybGQ=")
                        .withTlsCert("/%^$lkg")
                        .build()));
    }
}
