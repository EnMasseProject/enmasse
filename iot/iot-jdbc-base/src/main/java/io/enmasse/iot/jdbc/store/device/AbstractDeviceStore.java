/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.device;

import java.util.Optional;


import org.eclipse.hono.deviceregistry.service.device.DeviceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.jdbc.store.AbstractStore;
import io.enmasse.iot.jdbc.store.Statement;
import io.enmasse.iot.jdbc.store.StatementConfiguration;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vertx.core.Future;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLOperations;

public abstract class AbstractDeviceStore extends AbstractStore {

    private static final Logger log = LoggerFactory.getLogger(AbstractDeviceStore.class);

    protected final SQLClient client;
    protected final Tracer tracer;

    private final Statement readRegistrationStatement;

    public AbstractDeviceStore(final SQLClient client, final Tracer tracer, final StatementConfiguration cfg) {
        super(client, tracer, cfg.getStatement("checkConnection"));

        this.client = client;
        this.tracer = tracer;

        this.readRegistrationStatement = cfg
                .getRequiredStatment("readRegistration")
                .validateParameters(
                        "tenant_id",
                        "device_id");
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
}
