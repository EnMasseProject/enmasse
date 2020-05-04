/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import static io.enmasse.iot.jdbc.config.JdbcProperties.dataSource;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import io.enmasse.iot.jdbc.config.JdbcDeviceProperties;
import io.enmasse.iot.jdbc.config.JdbcProperties;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;

public final class DeviceStores {

    private DeviceStores() {
    }

    public static StoreFactory<AbstractDeviceAdapterStore> adapterStoreFactory() {
        return AdapterStoreFactory.INSTANCE;
    }

    public static StoreFactory<AbstractDeviceManagementStore> managementStoreFactory() {
        return ManagementStoreFactory.INSTANCE;
    }

    public static interface StoreFactory<T extends AbstractDeviceStore> {
        public T createTable(final Vertx vertx, final Tracer tracer, final JdbcProperties properties, final Optional<String> credentials, final Optional<String> registrations) throws IOException;
    }

    private static final class AdapterStoreFactory implements StoreFactory<AbstractDeviceAdapterStore> {

        private static final StoreFactory<AbstractDeviceAdapterStore> INSTANCE = new AdapterStoreFactory();

        private AdapterStoreFactory() {
        }

        @Override
        public AbstractDeviceAdapterStore createTable(final Vertx vertx, final Tracer tracer, final JdbcProperties properties, final Optional<String> credentials, final Optional<String> registrations) throws IOException {
            return new TableAdapterStore(
                    dataSource(vertx, properties),
                    tracer,
                    Configurations.tableConfiguration(properties.getUrl(), credentials, registrations));
        }
    }

    private static final class ManagementStoreFactory implements StoreFactory<AbstractDeviceManagementStore> {

        private static final StoreFactory<AbstractDeviceManagementStore> INSTANCE = new ManagementStoreFactory();

        private ManagementStoreFactory () {
        }

        @Override
        public AbstractDeviceManagementStore createTable(final Vertx vertx, final Tracer tracer, final JdbcProperties properties, final Optional<String> credentials, final Optional<String> registrations) throws IOException {
            return new TableManagementStore(
                    dataSource(vertx, properties),
                    tracer,
                    Configurations.tableConfiguration(properties.getUrl(), credentials, registrations));
        }
    }

    public static <T extends AbstractDeviceStore> T store(final Vertx vertx, final Tracer tracer, final JdbcDeviceProperties deviceProperties, final Function<JdbcDeviceProperties, JdbcProperties> extractor, final StoreFactory<T> factory) throws IOException {

        var properties = extractor.apply(deviceProperties);

        var prefix = Optional.ofNullable(properties.getTableName());
        var credentials = prefix.map(s -> s + "_credentials");
        var registrations = prefix.map(s -> s + "_registrations");

        return factory.createTable(vertx, tracer, properties, credentials, registrations);

    }

}
