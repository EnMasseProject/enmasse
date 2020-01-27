/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.jdbc.store.AbstractStore;
import io.enmasse.iot.jdbc.store.SQL;
import io.enmasse.iot.jdbc.store.Statement;
import io.enmasse.iot.jdbc.store.StatementConfiguration;
import io.enmasse.iot.registry.device.CredentialKey;
import io.enmasse.iot.registry.device.DeviceKey;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLOperations;
import io.vertx.ext.sql.UpdateResult;

public abstract class AbstractDeviceStore extends AbstractStore {

    private static final Logger log = LoggerFactory.getLogger(AbstractDeviceStore.class);

    protected final SQLClient client;
    protected final Tracer tracer;

    private final Statement createStatement;

    private final Statement readRegistrationStatement;
    private final Statement updateRegistrationStatement;
    private final Statement updateRegistrationVersionedStatement;
    private final Statement deleteStatement;
    private final Statement deleteVersionedStatement;
    private final Statement dropTenantStatement;

    public abstract Future<Optional<CredentialsReadResult>> findCredentials(CredentialKey key, SpanContext spanContext);

    public abstract Future<Optional<CredentialsReadResult>> getCredentials(DeviceKey key, SpanContext spanContext);

    public abstract Future<Boolean> setCredentials(DeviceKey key, List<CommonCredential> credentials, Optional<String> resourceVersion, SpanContext spanContext);

    public AbstractDeviceStore(final SQLClient client, final Tracer tracer, final StatementConfiguration cfg) throws IOException {
        super(client, tracer, cfg.getStatement("checkConnection"));
        this.client = client;
        this.tracer = tracer;

        this.createStatement = cfg
                .getRequiredStatment("create")
                .validateParameters(
                        "tenant_id",
                        "device_id",
                        "version",
                        "data");

        this.readRegistrationStatement = cfg
                .getRequiredStatment("readRegistration")
                .validateParameters(
                        "tenant_id",
                        "device_id");

        this.updateRegistrationStatement = cfg
                .getRequiredStatment("updateRegistration")
                .validateParameters(
                        "tenant_id",
                        "device_id",
                        "next_version",
                        "data");

        this.updateRegistrationVersionedStatement = cfg
                .getRequiredStatment("updateRegistrationVersioned")
                .validateParameters(
                        "tenant_id",
                        "device_id",
                        "next_version",
                        "data",
                        "expected_version");

        this.deleteStatement = cfg
                .getRequiredStatment("delete")
                .validateParameters(
                        "tenant_id",
                        "device_id");

        this.deleteVersionedStatement = cfg
                .getRequiredStatment("deleteVersioned")
                .validateParameters(
                        "tenant_id",
                        "device_id",
                        "expected_version");

        this.dropTenantStatement = cfg
                .getRequiredStatment("dropTenant")
                .validateParameters(
                        "tenant_id");

    }

    public Future<UpdateResult> createDevice(final DeviceKey key, final Device device, final SpanContext spanContext) {

        final String json = Json.encode(device);

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "create device")
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .withTag("data", json)
                .start();

        var expanded = this.createStatement.expand(params -> {
            params.put("tenant_id", key.getTenantId());
            params.put("device_id", key.getDeviceId());
            params.put("version", UUID.randomUUID().toString());
            params.put("data", json);
        });

        log.debug("createDevice - statement: {}", expanded);
        var f = expanded.trace(this.tracer, span).update(this.client)
                .recover(SQL::translateException);

        return MoreFutures
                .whenComplete(f, span::finish);
    }

    protected Future<UpdateResult> updateJsonField(final DeviceKey key, final Statement statement, final String jsonValue, final Optional<String> resourceVersion,
            final Span span) {

        var expanded = statement.expand(map -> {
            map.put("tenant_id", key.getTenantId());
            map.put("device_id", key.getDeviceId());
            map.put("next_version", UUID.randomUUID().toString());
            map.put("data", jsonValue);
            resourceVersion.ifPresent(version -> map.put("expected_version", version));
        });

        log.debug("update - statement: {}", expanded);

        // execute update
        var result = expanded.trace(this.tracer, span).update(this.client);

        // process result, check optimistic lock
        return checkOptimisticLock(result, span, this::readDevice, key, resourceVersion);
    }

    public Future<UpdateResult> updateDevice(final DeviceKey key, final Device device, final Optional<String> resourceVersion, final SpanContext spanContext) {

        final String json = Json.encode(device);

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "update device")
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .withTag("data", json)
                .start();

        resourceVersion.ifPresent(version -> span.setTag("version", version));

        final Statement statement = resourceVersion.isPresent() ? this.updateRegistrationVersionedStatement : this.updateRegistrationStatement;
        var f = updateJsonField(key, statement, json, resourceVersion, span);

        return MoreFutures
                .whenComplete(f, span::finish);

    }

    protected Future<ResultSet> readDevice(final DeviceKey key, final Span span) {
        return readDevice(this.client, key, span);
    }

    protected Future<ResultSet> readDevice(final SQLOperations operations, final DeviceKey key, final Span span) {
        return read(operations, key, this.readRegistrationStatement, span);
    }

    protected Future<ResultSet> read(final SQLOperations operations, final DeviceKey key, final Statement statement, final Span span) {
        return read(operations, key, Optional.empty(), statement, span);
    }

    protected Future<ResultSet> read(final SQLOperations operations, final DeviceKey key, final Optional<String> resourceVersion, final Statement statement, final Span span) {

        var expanded = statement.expand(params -> {
            params.put("tenant_id", key.getTenantId());
            params.put("device_id", key.getDeviceId());
            resourceVersion.ifPresent(version -> params.put("expected_version", version));
        });

        log.debug("read - statement: {}", expanded);

        return expanded.trace(this.tracer, span).query(this.client);
    }

    public Future<Optional<DeviceReadResult>> readDevice(final DeviceKey key, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "read device")
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .start();

        var f = read(this.client, key, this.readRegistrationStatement, span)
                .<Optional<DeviceReadResult>>flatMap(r -> {
                    var entries = r.getRows(true);
                    switch (entries.size()) {
                        case 0:
                            return Future.succeededFuture((Optional.empty()));
                        case 1:
                            var entry = entries.get(0);
                            var device = Json.decodeValue(entry.getString("data"), Device.class);
                            var version = Optional.ofNullable(entry.getString("version"));
                            return Future.succeededFuture(Optional.of(new DeviceReadResult(device, version)));
                        default:
                            return Future.failedFuture(new IllegalStateException("Found multiple entries for a single device"));
                    }
                });

        return MoreFutures
                .whenComplete(f, span::finish);

    }

    public Future<UpdateResult> deleteDevice(final DeviceKey key, final Optional<String> resourceVersion, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "delete device")
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .start();

        resourceVersion.ifPresent(version -> span.setTag("version", version));

        final Statement statement;
        if (resourceVersion.isPresent()) {
            statement = this.deleteVersionedStatement;
        } else {
            statement = this.deleteStatement;
        }

        var expanded = statement.expand(map -> {
            map.put("tenant_id", key.getTenantId());
            map.put("device_id", key.getDeviceId());
            resourceVersion.ifPresent(version -> map.put("expected_version", version));
        });

        log.debug("delete - statement: {}", expanded);

        var result = expanded.trace(this.tracer, span).update(this.client);

        return MoreFutures
                .whenComplete(result, span::finish);

    }

    public Future<UpdateResult> dropTenant(final String tenantId, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "drop tenant")
                .withTag("tenant_instance_id", tenantId)
                .start();

        var expanded = this.dropTenantStatement.expand(params -> {
            params.put("tenant_id", tenantId);
        });

        log.debug("delete - statement: {}", expanded);

        var result = expanded.update(this.client);

        return MoreFutures
                .whenComplete(result, span::finish);

    }

}
