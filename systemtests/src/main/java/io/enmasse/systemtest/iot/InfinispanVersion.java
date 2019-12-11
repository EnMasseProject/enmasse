/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.util.Optional;

public enum InfinispanVersion {
    V9, V10;

    /**
     * Get the version to use for infinispan.
     *
     * @return The version to use, never returns {@code null}.
     * @throws IllegalArgumentException If a value is set, but cannot be mapped to an enum literal.
     */
    public static InfinispanVersion current() {
        return Optional
                .ofNullable(System.getenv("INFINISPAN_VERSION"))
                .map(InfinispanVersion::valueOf)
                .orElse(InfinispanVersion.V9);
    }
}
