/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.eclipse.hono.config.ServerConfig;
import org.eclipse.hono.service.VertxBasedHealthCheckServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;

@Configuration
public class HealthConfiguration {

    private static final String QUALIFIER = "health";

    @Bean
    @Qualifier(QUALIFIER)
    @ConfigurationProperties(prefix = "enmasse.iot.health-check")
    public ServerConfig healthCheckConfigProperties() {
        return new ServerConfig();
    }

    @Bean
    public VertxBasedHealthCheckServer healthCheckServer (final Vertx vertx, @Qualifier(QUALIFIER) final ServerConfig config) {
        return new VertxBasedHealthCheckServer(vertx, config);
    }

}
