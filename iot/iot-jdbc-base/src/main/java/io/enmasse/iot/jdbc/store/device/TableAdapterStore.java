/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.jdbc.store.Statement;
import io.enmasse.iot.jdbc.store.StatementConfiguration;
import io.enmasse.iot.registry.device.CredentialKey;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.sql.SQLClient;

public class TableAdapterStore extends AbstractDeviceAdapterStore {

    private static final Logger log = LoggerFactory.getLogger(TableAdapterStore.class);

    private final Statement findCredentialsStatement;

    public TableAdapterStore(final SQLClient client, final Tracer tracer, final StatementConfiguration cfg) throws IOException {
        super(client, tracer, cfg);
        cfg.dump(log);

        this.findCredentialsStatement = cfg
                .getRequiredStatment("findCredentials")
                .validateParameters(
                        "tenant_id",
                        "type",
                        "auth_id");

    }

    @Override
    public Future<Optional<CredentialsReadResult>> findCredentials(final CredentialKey key, final SpanContext spanContext) {

        final Span span = TracingHelper.buildChildSpan(this.tracer, spanContext, "find credentials")
                .withTag("auth_id", key.getAuthId())
                .withTag("type", key.getType())
                .withTag("tenant_instance_id", key.getTenantId())
                .start();

        var expanded = this.findCredentialsStatement.expand(params -> {
            params.put("tenant_id", key.getTenantId());
            params.put("type", key.getType());
            params.put("auth_id", key.getAuthId());
        });

        log.debug("findCredentials - statement: {}", expanded);
        var f = expanded.trace(this.tracer, span).query(this.client)
                .<Optional<CredentialsReadResult>>flatMap(r -> {
                    var entries = r.getRows(true);
                    span.log(Map.of(
                            "event", "read result",
                            "rows", entries.size()));

                    final Set<String> deviceIds = entries.stream()
                            .map(o -> o.getString("device_id"))
                            .filter(o -> o != null)
                            .collect(Collectors.toSet());

                    int num = deviceIds.size();
                    if (num <= 0) {
                        return Future.succeededFuture(Optional.empty());
                    } else if (num > 1) {
                        TracingHelper.logError(span, "Found multiple entries for a single device");
                        return Future.failedFuture(new IllegalStateException("Found multiple entries for a single device"));
                    }

                    // we know now that we have exactly one entry
                    final String deviceId = deviceIds.iterator().next();

                    final List<CommonCredential> credentials = entries.stream()
                            .map(o -> o.getString("data"))
                            .map(s -> Json.decodeValue(s, CommonCredential.class))
                            .collect(Collectors.toList());

                    return Future.succeededFuture(Optional.of(new CredentialsReadResult(deviceId, credentials, Optional.empty())));
                });

        return MoreFutures
                .whenComplete(f, span::finish);

    }

}
