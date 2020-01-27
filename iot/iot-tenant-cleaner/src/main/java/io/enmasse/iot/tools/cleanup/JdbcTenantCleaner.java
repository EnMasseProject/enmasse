/*
 *  Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import static io.enmasse.iot.jdbc.config.JdbcProperties.dataSource;
import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.jdbc.config.JdbcDeviceProperties;
import io.enmasse.iot.jdbc.config.JdbcProperties;
import io.enmasse.iot.jdbc.store.devcon.Store;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceStore;
import io.enmasse.iot.jdbc.store.device.JsonStore;
import io.enmasse.iot.jdbc.store.device.TableStore;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopTracerFactory;
import io.vertx.core.Vertx;
import io.vertx.ext.sql.UpdateResult;

public class JdbcTenantCleaner implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(JdbcTenantCleaner.class);

    private final Vertx vertx;
    private final String tenantId;
    private final AbstractDeviceStore devices;
    private final io.enmasse.iot.jdbc.store.devcon.Store deviceInformation;

    public JdbcTenantCleaner() throws Exception {

        this.vertx = Vertx.vertx();

        final ObjectMapper mapper = new ObjectMapper();
        this.tenantId = System.getenv("tenantId");

        final JdbcDeviceProperties devices = mapper.readValue(System.getenv("jdbc.devices"), JdbcDeviceProperties.class);
        final JdbcProperties deviceInformation = mapper.readValue(System.getenv("jdbc.deviceInformation"), JdbcProperties.class);

        switch (devices.getMode()) {
            case JSON_FLAT:
                this.devices = new io.enmasse.iot.jdbc.store.device.JsonStore(
                        dataSource(vertx, devices),
                        NoopTracerFactory.create(),
                        false,
                        JsonStore.defaultConfiguration(devices.getUrl(), Optional.ofNullable(devices.getTableName()), false));
                break;
            case JSON_TREE:
                this.devices = new io.enmasse.iot.jdbc.store.device.JsonStore(
                        dataSource(vertx, devices),
                        NoopTracerFactory.create(),
                        true,
                        JsonStore.defaultConfiguration(devices.getUrl(), ofNullable(devices.getTableName()), true));
                break;
            case TABLE:
                var prefix = Optional.ofNullable(devices.getTableName());
                var registrations = prefix.map(s -> s + "_registrations");
                var credentials = prefix.map(s -> s + "_credentials");
                this.devices = new io.enmasse.iot.jdbc.store.device.TableStore(
                        dataSource(this.vertx, devices),
                        NoopTracerFactory.create(),
                        TableStore.defaultConfiguration(devices.getUrl(), credentials, registrations));
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown storage mode: %s", devices.getMode()));
        }

        this.deviceInformation = new io.enmasse.iot.jdbc.store.devcon.Store(
                dataSource(vertx, deviceInformation),
                NoopTracerFactory.create(),
                Store.defaultStatementConfiguration(deviceInformation.getUrl(), ofNullable(deviceInformation.getTableName())));

    }

    public void run() throws Exception {

        var f1 = this.devices.dropTenant(this.tenantId, NoopSpan.INSTANCE.context());

        var part1 = MoreFutures.map(f1)
                .whenComplete((r, e) -> logResult("Devices", r, e));

        var f2 = this.deviceInformation.dropTenant(this.tenantId, NoopSpan.INSTANCE.context());
        var part2 = MoreFutures.map(f2)
                .whenComplete((r, e) -> logResult("Devices Information", r, e));

        CompletableFuture
                .allOf(part1, part2)
                .get();
    }

    private void logResult(final String operation, final UpdateResult result, final Throwable error) {
        if (error == null) {
            log.info("{}: Cleaned up, deleted records: {}", operation, result.getUpdated());
        } else {
            log.warn("{}: Failed to clean up", operation, error);
        }
    }

    @Override
    public void close() throws Exception {
        this.devices.close();
        this.deviceInformation.close();
        this.vertx.close();
    }

}
