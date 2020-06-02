/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(final String args[]) throws Exception {

        log.info("Starting tenant cleanup");

        cleanupDeviceRegistry();
        cleanupDeviceConnection();

        log.info("Finished tenant cleanup");

        System.exit(0);

    }

    private static void cleanupDeviceConnection() throws Exception {
        final String connectionType = System.getenv()
                .getOrDefault("deviceConnection.type", "<missing>");

        switch (connectionType) {
            case "noop":
                // nothing to clean up
                break;
            case "jdbc":
                try (JdbcDeviceConnectionTenantCleaner app = new JdbcDeviceConnectionTenantCleaner()) {
                    app.run();
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Illegal connection type: '%s'", connectionType));
        }
    }

    private static void cleanupDeviceRegistry() throws Exception {
        final String registryTpe = System.getenv()
                .getOrDefault("registry.type", "<missing>");

        switch (registryTpe) {
            case "noop":
                // nothing to clean up
                break;
            case "infinispan":
                try (InfinispanDeviceRegistryCleaner app = new InfinispanDeviceRegistryCleaner()) {
                    app.run();
                }
                break;
            case "jdbc":
                try (JdbcDeviceRegsitryTenantCleaner app = new JdbcDeviceRegsitryTenantCleaner()) {
                    app.run();
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Illegal registry type: '%s'", registryTpe));
        }
    }
}
