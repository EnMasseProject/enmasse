/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.framework.TestTag.FRAMEWORK;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

@Tag(FRAMEWORK)
public class CredentialsTest {

    @Test
    void testCreateSecret() {
        var creds = JsonObject.mapFrom(CredentialsRegistryClient.createCredentialsObject("auth-id", "foo", null));

        var secrets = creds.getJsonArray("secrets");
        assertNotNull(secrets);
        var secret = secrets.getJsonObject(0);
        assertNotNull(secret);

        assertNotNull(secret.getBinary("pwd-hash"));
        assertNotNull(secret.getBinary("salt"));
    }

}
