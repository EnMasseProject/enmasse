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
import org.eclipse.hono.service.amqp.AmqpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.tenant.config.AmqpEndpointConfiguration;
import io.enmasse.iot.tenant.impl.TenantAmqpService;
import io.opentracing.Tracer;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;

@Dependent
public class TenantAmqpServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckServer.class);

    @Inject
    AmqpEndpointConfiguration configuration;
    @Inject
    HealthCheckServer healthCheckServer;
    @Inject
    ProtonSaslAuthenticatorFactory factory;
    @Inject
    Tracer tracer;

    @Singleton
    public TenantAmqpService tenantAmqpService(final Instance<AmqpEndpoint> endpoints) {
        var result = new TenantAmqpService();
        result.setConfig(this.configuration.toHono());
        result.setHealthCheckServer(this.healthCheckServer);
        result.setSaslAuthenticatorFactory(this.factory);
        result.setTracer(this.tracer);
        var endpointsList = endpoints.stream().collect(Collectors.toList());
        log.info("Adding endpoints: {}", endpointsList);
        result.addEndpoints(endpointsList);
        return result;
    }

}
