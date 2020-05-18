/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.devcon;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hono.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.jdbc.store.AbstractStore;
import io.enmasse.iot.jdbc.store.SQL;
import io.enmasse.iot.jdbc.store.Statement;
import io.enmasse.iot.jdbc.store.StatementConfiguration;
import io.enmasse.iot.registry.devcon.DeviceConnectionKey;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.vertx.core.Future;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;

public class Store extends AbstractStore {

    public static final String DEFAULT_TABLE_NAME = "device_states";

    private final static Logger log = LoggerFactory.getLogger(Store.class);

    private final SQLClient client;
    private final Tracer tracer;

    private final Statement readStatement;
    private final Statement dropTenantStatement;

    private final Statement updateLastKnownGatewayStatement;
    private final Statement updateAdapterInstanceStatement;

    public static StatementConfiguration defaultStatementConfiguration(final String jdbcUrl, final Optional<String> tableName) throws IOException {

        final String dialect = SQL.getDatabaseDialect(jdbcUrl);
        final String tableNameString = tableName.orElse(DEFAULT_TABLE_NAME);

        return StatementConfiguration
                .empty(tableNameString)
                .overideWithDefaultPattern("base", dialect, Store.class, StatementConfiguration.DEFAULT_PATH.resolve("devcon"));

    }

    public Store(final SQLClient client, final Tracer tracer, final StatementConfiguration cfg) {
        super(client, tracer, cfg.getStatement("checkConnection"));
        cfg.dump(log);

        this.client = client;
        this.tracer = tracer;

        this.readStatement = cfg
                .getRequiredStatment("read")
                .validateParameters(
                        "tenant_id",
                        "device_id");

        this.dropTenantStatement = cfg.getRequiredStatment("dropTenant")
                .validateParameters("tenant_id");

        this.updateLastKnownGatewayStatement = cfg.getRequiredStatment("updateLastKnownGateway")
                .validateParameters(
                        "tenant_id",
                        "device_id",
                        "gateway_id");

        this.updateAdapterInstanceStatement = cfg.getRequiredStatment("updateAdapterInstance")
                .validateParameters(
                        "tenant_id",
                        "device_id",
                        "adapter_instance_id");

    }

    public Future<Optional<DeviceState>> readDeviceState(final DeviceConnectionKey key, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "read device state", getClass().getSimpleName())
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .start();

        var expanded = this.readStatement.expand(map -> {
            map.put("tenant_id", key.getTenantId());
            map.put("device_id", key.getDeviceId());
        });

        log.debug("readDeviceState - statement: {}", expanded);
        var result = expanded.trace(this.tracer, span).query(this.client);

        var f = result
                .<Optional<DeviceState>>flatMap(r -> {
                    var entries = r.getRows(true);
                    span.log(Map.of(
                            "event", "read result",
                            "rows", entries.size()));
                    switch (entries.size()) {
                        case 0:
                            return succeededFuture(Optional.empty());
                        case 1:
                            var entry = entries.get(0);
                            var state = new DeviceState();
                            state.setLastKnownGateway(Optional.ofNullable(entry.getString("last_known_gateway")));
                            state.setCommandHandlingAdapterInstance(Optional.ofNullable(entry.getString("adapter_instance_id")));
                            return succeededFuture(Optional.of(state));
                        default:
                            return failedFuture(new IllegalStateException("Found multiple entries for a single device"));
                    }
                });

        return MoreFutures
                .whenComplete(f, span::finish);

    }

    public Future<UpdateResult> setLastKnownGateway(final DeviceConnectionKey key, final String gatewayId, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "update device state", getClass().getSimpleName())
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .withTag("gateway_id", gatewayId)
                .start();

        var expanded = this.updateLastKnownGatewayStatement.expand(params -> {
            params.put("tenant_id", key.getTenantId());
            params.put("device_id", key.getDeviceId());
            params.put("gateway_id", gatewayId);
        });

        log.debug("setLastKnownGateway - statement: {}", expanded);
        var result = expanded.trace(this.tracer, span).update(this.client);

        return MoreFutures
                .whenComplete(result, span::finish);

    }

    public Future<UpdateResult> processSetCommandHandlingAdapterInstance(final DeviceConnectionKey key, final String adapterInstanceId, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "Set Command Handling Adapter Instance", getClass().getSimpleName())
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .withTag("adapter_instance_id", adapterInstanceId)
                .start();

        var expanded = this.updateAdapterInstanceStatement.expand(params -> {
            params.put("tenant_id", key.getTenantId());
            params.put("device_id", key.getDeviceId());
            params.put("adapter_instance_id", adapterInstanceId);
        });

        log.debug("setCommandHandlingAdapterInstance - statement: {}", expanded);

        var result = expanded.trace(this.tracer, span).update(this.client);

        return MoreFutures
                .whenComplete(result, span::finish);

    }

    public Future<UpdateResult> dropTenant(final String tenantId, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "drop tenant", getClass().getSimpleName())
                .withTag("tenant_instance_id", tenantId)
                .start();

        var expanded = this.dropTenantStatement.expand(params -> {
            params.put("tenant_id", tenantId);
        });

        log.debug("dropTenant - statement: {}", expanded);
        var result = expanded.trace(this.tracer, span).update(this.client);

        return MoreFutures
                .whenComplete(result, span::finish);
    }

}
