/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static org.junit.Assert.assertNotNull;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class CredentialsTest {

    @Test
    public void testCreateSecret() {
        var creds = JsonObject.mapFrom(CredentialsRegistryClient.createCredentialsObject("auth-id", "foo", null));

        var secrets = creds.getJsonArray("secrets");
        assertNotNull(secrets);
        var secret = secrets.getJsonObject(0);
        assertNotNull(secret);

        assertNotNull(secret.getBinary("pwd-hash"));
        assertNotNull(secret.getBinary("salt"));
    }

}
