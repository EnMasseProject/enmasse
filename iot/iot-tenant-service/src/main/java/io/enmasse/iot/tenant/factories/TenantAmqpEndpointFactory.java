/*
 * Copyright 2020 EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.factories;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.hono.service.amqp.AmqpEndpoint;
import org.eclipse.hono.service.tenant.DelegatingTenantAmqpEndpoint;
import org.eclipse.hono.service.tenant.TenantService;

import io.enmasse.iot.tenant.config.AmqpEndpointConfiguration;
import io.opentracing.Tracer;
import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;

@ApplicationScoped
public class TenantAmqpEndpointFactory {

    @Inject
    Tracer tracer;

    @Inject
    AmqpEndpointConfiguration configuration;

    @Inject
    TenantService service;

    @Inject
    Vertx vertx;

    @Singleton
    @Startup
    public AmqpEndpoint tenantAmqpEndpoint() {
        var result = new DelegatingTenantAmqpEndpoint<>(this.vertx, this.service);
        result.setConfiguration(configuration.toHono());
        result.setTracer(tracer);
        return result;
    }


}
