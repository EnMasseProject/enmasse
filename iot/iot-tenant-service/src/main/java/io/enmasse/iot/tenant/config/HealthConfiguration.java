/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.eclipse.hono.config.ApplicationConfigProperties;
import org.eclipse.hono.service.VertxBasedHealthCheckServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;

@Configuration
public class HealthConfiguration {

    @Bean
    public VertxBasedHealthCheckServer healthCheckServer (final Vertx vertx, final ApplicationConfigProperties config) {
        return new VertxBasedHealthCheckServer(vertx, config);
    }

}
