/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionAmqpEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;

@Configuration
public class DeviceConnectionServiceConfiguration {

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Device Connection</em> API.
     *
     * @return The handler.
     */
    @Bean
    @Autowired
    public DeviceConnectionAmqpEndpoint deviceConnectionAmqpEndpoint(final Vertx vertx) {
        return new DeviceConnectionAmqpEndpoint(vertx);
    }

}
