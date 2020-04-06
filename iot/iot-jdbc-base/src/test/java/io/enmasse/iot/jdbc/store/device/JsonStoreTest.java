/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.PskCredential;
import org.eclipse.hono.service.management.credentials.PskSecret;
import org.junit.jupiter.api.Test;

public class JsonStoreTest {

    private static PskCredential newPskCredential(final String authId) {

        final PskSecret psk1 = new PskSecret();
        psk1.setKey("foo".getBytes(StandardCharsets.UTF_8));

        final PskCredential result = new PskCredential();
        result.setAuthId(authId);
        result.setSecrets(Arrays.asList(psk1));

        return result;
    }

    @Test
    public void testHierarchical1() {

        final PskCredential psk = newPskCredential("auth-1");
        String encoded = JsonAdapterStore.encodeCredentialsHierarchical(Arrays.asList(psk));

        assertThat(encoded, is("{\"psk\":{\"auth-1\":{\"secrets\":[{\"key\":\"Zm9v\"}]}}}"));

        final List<CommonCredential> decoded = JsonAdapterStore.decodeCredentialsHierarchical(encoded);

        assertThat(decoded, hasSize(1));
    }

}
