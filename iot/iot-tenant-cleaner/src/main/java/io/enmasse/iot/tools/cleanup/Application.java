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
        String connectionType = System.getenv("deviceConnection.type");
        if (connectionType == null) {
            connectionType = "<missing>";
        }

        switch (connectionType) {
            case "infinispan":
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
        String registryTpe = System.getenv("registry.type");
        if (registryTpe == null) {
            registryTpe = "<missing>";
        }

        switch (registryTpe) {
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
