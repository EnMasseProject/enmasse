/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.keycloak.controller;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class KeycloakParamsTest {
    @Test
    public void testRequiredEnvironment() throws Exception {
        Map<String, String> env = new HashMap<>();
        assertThrows(env);

        env.put("KEYCLOAK_HOSTNAME", "localhost");
        assertThrows(env);

        env.put("KEYCLOAK_PORT", "1234");
        assertThrows(env);

        env.put("KEYCLOAK_ADMIN_USER", "admin");
        assertThrows(env);

        env.put("KEYCLOAK_ADMIN_PASSWORD", "password");

        env.put("KEYCLOAK_CERT", new String(Files.readAllBytes(new File("src/test/resources/ca.crt").toPath()), "UTF-8"));

        KeycloakParams params = KeycloakParams.fromEnv(env);
        assertEquals("localhost", params.getHost());
        assertEquals(1234, params.getHttpPort());
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
