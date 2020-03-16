/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import static io.enmasse.iot.jdbc.config.JdbcProperties.dataSource;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.jdbc.config.JdbcDeviceProperties;
import io.enmasse.iot.jdbc.config.JdbcProperties;
import io.enmasse.iot.jdbc.store.devcon.Store;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceStore;
import io.enmasse.iot.jdbc.store.device.JsonStore;
import io.enmasse.iot.jdbc.store.device.TableStore;
import io.enmasse.iot.utils.ConfigBase;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;

@Configuration
public class JdbcConfiguration {

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.jdbc.devices")
    public JdbcDeviceProperties devicesSourceProperties() {
        return new JdbcDeviceProperties();
    }

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.jdbc.device-information")
    public JdbcProperties deviceInformationSourceProperties() {
        return new JdbcProperties();
    }

    @Bean
    @Autowired
    public AbstractDeviceStore devicesStore(final Vertx vertx, final Tracer tracer) throws IOException {

        var properties = devicesSourceProperties();
        var jdbcUrl = properties.getUrl();

        switch (properties.getMode()) {
            case JSON_FLAT:
                return new JsonStore(
                        dataSource(vertx, properties),
                        tracer,
                        false,
                        JsonStore.defaultConfiguration(jdbcUrl, Optional.ofNullable(properties.getTableName()), false));
            case JSON_TREE:
                return new JsonStore(
                        dataSource(vertx, properties),
                        tracer,
                        true,
                        JsonStore.defaultConfiguration(jdbcUrl, Optional.ofNullable(properties.getTableName()), true));
            case TABLE:
                var prefix = Optional.ofNullable(properties.getTableName());
                var registrations = prefix.map(s -> s + "_registrations");
                var credentials = prefix.map(s -> s + "_credentials");
                return new TableStore(
                        dataSource(vertx, properties),
                        tracer,
                        TableStore.defaultConfiguration(jdbcUrl, credentials, registrations));
        }

        throw new IllegalStateException(String.format("Unknown store type: %s", properties.getMode()));
    }

    @Bean
    @Autowired
    public io.enmasse.iot.jdbc.store.devcon.Store deviceInformationStore(final Vertx vertx, final Tracer tracer) throws IOException {

        var properties = deviceInformationSourceProperties();
        var jdbcUrl = properties.getUrl();

        return new io.enmasse.iot.jdbc.store.devcon.Store(
                dataSource(vertx, properties),
                tracer,
                Store.defaultStatementConfiguration(jdbcUrl, Optional.ofNullable(properties.getTableName())));

    }

}
