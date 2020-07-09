/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.utils.TestUtils;

import java.util.UUID;

public final class Names {

    private Names() {
    }

    /**
     * Create a random Kubernetes name.
     *
     * @return A name, suitable for a normal Kubernetes resource.
     */
    public static String randomName() {
        return UUID.randomUUID().toString();
    }

    /**
     * Create a random Kubernetes namespace name.
     *
     * @return A name, suitable for a Kubernetes namespace.
     */
    public static String randomNamespace() {
        var result = UUID.randomUUID().toString();
        // we currently implement a workaround for EnMasseProject/enmasse#4933
        return result.replaceFirst("^[^a-z]", "x");
    }

    /**
     * Create random name for Hono device.
     *
     * @return A name, suitable for a Hono device.
     */
    public static String randomDevice() {
        return TestUtils.randomCharacters(23 /* max MQTT client ID length */);
    }
}
