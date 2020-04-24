/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.hono.deviceregistry.service.credentials.CredentialKey;
import org.eclipse.hono.deviceregistry.service.device.DeviceKey;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.tracing.TracingHelper;

import io.enmasse.iot.jdbc.store.StatementConfiguration;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;

public abstract class AbstractDeviceAdapterStore extends AbstractDeviceStore {

    public abstract Future<Optional<CredentialsReadResult>> findCredentials(CredentialKey key, SpanContext spanContext);

    public AbstractDeviceAdapterStore(final SQLClient client, final Tracer tracer, final StatementConfiguration cfg) throws IOException {
        super(client, tracer, cfg);
    }

    protected Future<ResultSet> readDevice(final DeviceKey key, final Span span) {
        return readDevice(this.client, key, span);
    }

    public Future<Optional<DeviceReadResult>> readDevice(final DeviceKey key, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "read device", getClass().getSimpleName())
                .withTag("tenant_instance_id", key.getTenantId())
                .withTag("device_id", key.getDeviceId())
                .start();

        var f = readDevice(this.client, key, span)
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

}
