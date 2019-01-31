/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.enmasse.k8s.util.JULInitializingTest;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeycloakRealmParamsTest extends JULInitializingTest {

    private KubernetesServer server = new KubernetesServer(true, true);

    private KubernetesClient client;

    @BeforeEach
    void setup() {
        server.before();
        client = server.getClient();
    }

    @AfterEach
    void tearDown() {
        server.after();
    }

    @Test
    void testRequiredEnvironment() {
        client.configMaps().createNew()
                .editOrNewMetadata()
                .withName("myconfig")
                .endMetadata()
                .addToData("identityProviderUrl", "https://localhost:8443/auth")
                .addToData("identityProviderClientId", "myclient")
                .addToData("identityProviderClientSecret", "mysecret")
                .done();

        KeycloakRealmParams params = KeycloakRealmParams.fromKube(client, "myconfig");
        assertEquals("https://localhost:8443/auth", params.getIdentityProviderUrl());
        assertEquals("myclient", params.getIdentityProviderClientId());
        assertEquals("mysecret", params.getIdentityProviderClientSecret());
    }
}
