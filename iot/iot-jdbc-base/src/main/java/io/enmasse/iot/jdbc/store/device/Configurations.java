/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import static io.enmasse.iot.jdbc.store.StatementConfiguration.DEFAULT_PATH;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import io.enmasse.iot.jdbc.store.SQL;
import io.enmasse.iot.jdbc.store.StatementConfiguration;

public final class Configurations {

    private Configurations() {}

    public static final String DEFAULT_TABLE_NAME_JSON = "devices";

    public static final String DEFAULT_TABLE_NAME_CREDENTIALS = "device_credentials";
    public static final String DEFAULT_TABLE_NAME_REGISTRATIONS = "device_registrations";

    public static StatementConfiguration tableConfiguration(final String jdbcUrl, final Optional<String> tableNameCredentials, final Optional<String> tableNameRegistrations)
            throws IOException {

        final String dialect = SQL.getDatabaseDialect(jdbcUrl);

        final String tableNameCredentialsString = tableNameCredentials.orElse(DEFAULT_TABLE_NAME_CREDENTIALS);
        final String tableNameRegistrationsString = tableNameRegistrations.orElse(DEFAULT_TABLE_NAME_REGISTRATIONS);

        return StatementConfiguration
                .empty(tableNameRegistrationsString, tableNameCredentialsString)
                .overideWithDefaultPattern("base", dialect, Configurations.class, StatementConfiguration.DEFAULT_PATH.resolve("device"))
                .overideWithDefaultPattern("table", dialect, Configurations.class, StatementConfiguration.DEFAULT_PATH.resolve("device"));

    }

    public static StatementConfiguration jsonConfiguration(final String jdbcUrl, final Optional<String> tableName, boolean hierarchical) throws IOException {

        final String dialect = SQL.getDatabaseDialect(jdbcUrl);
        final String tableNameString = tableName.orElse(DEFAULT_TABLE_NAME_JSON);
        final String jsonModel = hierarchical ? "json.tree" : "json.flat";

        final Path base = DEFAULT_PATH.resolve("device");

        return StatementConfiguration
                .empty(tableNameString)
                .overideWithDefaultPattern("base", dialect, Configurations.class, base)
                .overideWithDefaultPattern("json", dialect, Configurations.class, base)
                .overideWithDefaultPattern(jsonModel, dialect, Configurations.class, base);

    }

}
