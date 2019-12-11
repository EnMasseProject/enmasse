/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

import com.google.common.io.Resources;

public final class Infinispan {
    private Infinispan() {}

    public static Optional<String> version() {

        final Properties p = new Properties();
        final URL url = Resources.getResource("META-INF/infinispan-version.properties");

        try (InputStream stream = url.openStream()) {
            p.load(stream);
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.ofNullable(p.getProperty("infinispan.version"));

    }
}
