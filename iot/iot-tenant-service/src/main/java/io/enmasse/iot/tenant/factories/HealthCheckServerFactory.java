/*
 * Copyright 2020 EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.factories;

import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.hono.service.HealthCheckServer;
import org.eclipse.hono.service.VertxBasedHealthCheckServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.tenant.config.HealthConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@Dependent
public class HealthCheckServerFactory {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckServer.class);

    @Inject
    HealthConfiguration configugration;

    @Inject
    Vertx vertx;

    @Singleton
    public HealthCheckServer healthCheckServer(final Instance<Handler<Router>> additionalResources) {
        var result = new VertxBasedHealthCheckServer(this.vertx, this.configugration.toHono());
        var resources = additionalResources.stream().collect(Collectors.toList());
        log.info("Adding additional resources: {}", resources);
        result.setAdditionalResources(resources);
        return result;
    }

}
