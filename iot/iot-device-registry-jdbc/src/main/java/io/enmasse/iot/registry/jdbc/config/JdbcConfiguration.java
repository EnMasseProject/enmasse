/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import static io.enmasse.iot.jdbc.config.JdbcProperties.dataSource;
import static io.enmasse.iot.jdbc.store.device.DeviceStores.adapterStoreFactory;
import static io.enmasse.iot.jdbc.store.device.DeviceStores.managementStoreFactory;
import static io.enmasse.iot.jdbc.store.device.DeviceStores.store;
import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_DEVICE_CONNECTION;
import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_ADAPTER;
import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_MANAGEMENT;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.enmasse.iot.jdbc.config.JdbcDeviceProperties;
import io.enmasse.iot.jdbc.config.JdbcProperties;
import io.enmasse.iot.jdbc.store.devcon.Store;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceAdapterStore;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceManagementStore;
import io.enmasse.iot.utils.ConfigBase;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;

@Configuration
public class JdbcConfiguration {

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.jdbc")
    @Profile({PROFILE_REGISTRY_ADAPTER, PROFILE_REGISTRY_MANAGEMENT})
    public JdbcDeviceProperties devicesProperties() {
        return new JdbcDeviceProperties();
    }

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".device-connection.jdbc")
    @Profile(PROFILE_DEVICE_CONNECTION)
    public JdbcProperties deviceInformationSourceProperties() {
        return new JdbcProperties();
    }

    @Autowired
    @Bean
    @Profile(PROFILE_REGISTRY_ADAPTER)
    public AbstractDeviceAdapterStore devicesAdapterStore(final Vertx vertx, final Tracer tracer) throws IOException {
        return store(vertx, tracer, devicesProperties(), JdbcDeviceProperties::getAdapter, adapterStoreFactory());
    }

    @Autowired
    @Bean
    @Profile(PROFILE_REGISTRY_MANAGEMENT)
    public AbstractDeviceManagementStore devicesManagementStore(final Vertx vertx, final Tracer tracer) throws IOException {
        return store(vertx, tracer, devicesProperties(), JdbcDeviceProperties::getManagement, managementStoreFactory());
    }

    @Autowired
    @Bean
    @Profile(PROFILE_DEVICE_CONNECTION)
    public Store deviceInformationStore(final Vertx vertx, final Tracer tracer) throws IOException {

        var properties = deviceInformationSourceProperties();
        var jdbcUrl = properties.getUrl();

        return new io.enmasse.iot.jdbc.store.devcon.Store(
                dataSource(vertx, properties),
                tracer,
                Store.defaultStatementConfiguration(jdbcUrl, Optional.ofNullable(properties.getTableName())));

    }

}
