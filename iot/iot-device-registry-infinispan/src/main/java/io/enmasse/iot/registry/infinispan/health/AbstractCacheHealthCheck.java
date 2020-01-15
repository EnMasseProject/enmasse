/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.health;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.ServerStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.Status;

public abstract class AbstractCacheHealthCheck extends AbstractSyncHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(AbstractCacheHealthCheck.class);

    private final RemoteCache<?, ?> cache;

    public AbstractCacheHealthCheck(final String cacheName, final RemoteCache<?, ?> cache, final Vertx vertx) {
        super(vertx, "cache/" + cacheName);
        this.cache = cache;
    }

    /**
     * Check if the server is alive.
     * @return The status result.
     */
    @Override
    protected Status checkLivenessSync() {
        try {
            final ServerStatistics result = this.cache.serverStatistics();
            if (result == null) {
                log.info("Cache health liveness failed: No server info");
                return KO("No server info", null);
            }
        } catch (final Exception e) {
            log.info("Cache health liveness failed: {}", e);
            return KO("Failed to ping server", e);
        }
        return Status.OK();
    }
}
