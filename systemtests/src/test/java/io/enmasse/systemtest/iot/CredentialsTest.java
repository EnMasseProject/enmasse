/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CredentialsTest {

    @Test
    public void testCreateSecret() {
        var creds = CredentialsRegistryClient.createCredentialsObject("device-id", "auth-id", "foo", null);
        assertEquals("device-id", creds.getString("device-id"));

        var secrets = creds.getJsonArray("secrets");
        assertNotNull(secrets);
        var secret = secrets.getJsonObject(0);
        assertNotNull(secret);

        assertNotNull(secret.getBinary("pwd-hash"));
        assertNotNull(secret.getBinary("salt"));
    }

}
