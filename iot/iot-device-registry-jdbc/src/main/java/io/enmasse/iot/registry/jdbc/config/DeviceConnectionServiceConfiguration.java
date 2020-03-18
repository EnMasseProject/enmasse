/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_DEVICE_CONNECTION;

import org.eclipse.hono.service.deviceconnection.DeviceConnectionAmqpEndpoint;
import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.vertx.core.Vertx;

@Configuration
@Profile(PROFILE_DEVICE_CONNECTION)
public class DeviceConnectionServiceConfiguration {

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Device Connection</em> API.
     *
     * @return The handler.
     */
    @Autowired
    @Bean
    @ConditionalOnBean(DeviceConnectionService.class)
    public DeviceConnectionAmqpEndpoint deviceConnectionAmqpEndpoint(final Vertx vertx) {
        return new DeviceConnectionAmqpEndpoint(vertx);
    }

}
