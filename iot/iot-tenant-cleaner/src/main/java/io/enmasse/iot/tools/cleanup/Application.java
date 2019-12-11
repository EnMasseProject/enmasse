/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.infinispan.Infinispan;
import io.enmasse.iot.tools.cleanup.config.CleanerConfig;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(final String args[]) throws Exception {

        log.info("Infinispan Client Version: {}", Infinispan.version().orElse("<unknown>"));

        final Optional<Path> configFile = Arrays
                .asList(args)
                .stream()
                .findFirst()
                .map(Path::of);

        log.debug("config file: {}", configFile);

        try (InfinispanTenantCleaner app = new InfinispanTenantCleaner(CleanerConfig.load(configFile))) {
            app.run();
        }

    }
}
