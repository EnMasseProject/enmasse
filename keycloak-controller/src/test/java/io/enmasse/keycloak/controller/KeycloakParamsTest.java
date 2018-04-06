/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class KeycloakParamsTest {
    @Test
    public void testRequiredEnvironment() throws Exception {
        Map<String, String> env = new HashMap<>();
        assertThrows(env);

        env.put("KEYCLOAK_URI", "https://localhost:8443/auth");
        assertThrows(env);

        env.put("KEYCLOAK_ADMIN_USER", "admin");
        assertThrows(env);

        env.put("KEYCLOAK_ADMIN_PASSWORD", "password");

        env.put("KEYCLOAK_CERT", new String(Files.readAllBytes(new File("src/test/resources/ca.crt").toPath()), "UTF-8"));

        KeycloakParams params = KeycloakParams.fromEnv(env);
        assertEquals("https://localhost:8443/auth", params.getKeycloakUri());
        assertEquals("admin", params.getAdminUser());
        assertEquals("password", params.getAdminPassword());
    }

    private static void assertThrows(Map<String, String> env) throws Exception {
       try {
            KeycloakParams.fromEnv(env);
            fail("Should fail without any environment variables set");
        } catch (IllegalArgumentException ignored) {}
    }
}
