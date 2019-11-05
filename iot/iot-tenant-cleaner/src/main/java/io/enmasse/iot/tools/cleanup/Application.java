/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(final String args[]) throws Exception {

        final Optional<Path> configFile;

        if (args.length > 1) {
            configFile = Optional.of(Path.of(args[0]));
            log.debug("config file specified: {}", configFile);
        } else {
            configFile = Optional.empty();
        }

        try (InfinispanTenantCleaner app = new InfinispanTenantCleaner(configFile)) {
            app.run();
        }

    }
}
