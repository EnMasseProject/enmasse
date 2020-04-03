/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import static io.enmasse.iot.jdbc.config.JdbcProperties.dataSource;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.jdbc.config.JdbcProperties;
import io.enmasse.iot.jdbc.store.devcon.Store;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopTracerFactory;

public class JdbcDeviceConnectionTenantCleaner extends AbstractJdbcCleaner {

    private final io.enmasse.iot.jdbc.store.devcon.Store deviceConnection;

    public JdbcDeviceConnectionTenantCleaner() throws Exception {

        final ObjectMapper mapper = new ObjectMapper();
        final JdbcProperties deviceConnection = mapper.readValue(System.getenv("jdbc.deviceConnection"), JdbcProperties.class);

        this.deviceConnection = new io.enmasse.iot.jdbc.store.devcon.Store(
                dataSource(this.vertx, deviceConnection),
                NoopTracerFactory.create(),
                Store.defaultStatementConfiguration(deviceConnection.getUrl(), ofNullable(deviceConnection.getTableName())));

    }

    public void run() throws Exception {

        var f = this.deviceConnection.dropTenant(this.tenantId, NoopSpan.INSTANCE.context());
        MoreFutures.map(f)
                .whenComplete((r, e) -> logResult("Devices Connection", r, e))
                .get();

    }

    @Override
    public void close() throws Exception {
        this.deviceConnection.close();
        super.close();
    }

}
