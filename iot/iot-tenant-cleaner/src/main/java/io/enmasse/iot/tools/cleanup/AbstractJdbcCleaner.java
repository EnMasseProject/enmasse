/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.ext.sql.UpdateResult;

public abstract class AbstractJdbcCleaner implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(AbstractJdbcCleaner.class);

    protected final Vertx vertx;
    protected final String tenantId;

    public AbstractJdbcCleaner() {
        this.vertx = Vertx.vertx();
        this.tenantId = System.getenv("tenantId");
    }

    protected void logResult(final String operation, final UpdateResult result, final Throwable error) {
        if (error == null) {
            log.info("{}: Cleaned up, deleted records: {}", operation, result.getUpdated());
        } else {
            log.warn("{}: Failed to clean up", operation, error);
        }
    }

    @Override
    public void close() throws Exception {
        this.vertx.close();
    }

}
