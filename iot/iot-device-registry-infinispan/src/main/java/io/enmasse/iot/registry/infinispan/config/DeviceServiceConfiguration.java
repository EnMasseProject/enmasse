/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import org.eclipse.hono.service.credentials.CredentialsAmqpEndpoint;
import org.eclipse.hono.service.management.credentials.CredentialsManagementHttpEndpoint;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.device.DeviceManagementHttpEndpoint;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.registration.RegistrationAmqpEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;

@Configuration
public class DeviceServiceConfiguration {

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Device Registration</em> API.
     *
     * @return The handler.
     */
    @Bean
    @Autowired
    public RegistrationAmqpEndpoint registrationAmqpEndpoint(final Vertx vertx) {
        return new RegistrationAmqpEndpoint(vertx);
    }

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Credentials</em> API.
     *
     * @return The handler.
     */
    @Bean
    @Autowired
    public CredentialsAmqpEndpoint credentialsAmqpEndpoint(final Vertx vertx) {
        return new CredentialsAmqpEndpoint(vertx);
    }

    /**
     * Creates a new instance of an HTTP protocol handler for Hono's <em>Device Registration</em> API.
     *
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(DeviceManagementService.class)
    @Autowired
    public DeviceManagementHttpEndpoint registrationHttpEndpoint(final Vertx vertx) {
        return new DeviceManagementHttpEndpoint(vertx);
    }

    /**
     * Creates a new instance of an HTTP protocol handler for Hono's <em>Credentials</em> API.
     *
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(CredentialsManagementService.class)
    @Autowired
    public CredentialsManagementHttpEndpoint credentialsHttpEndpoint(final Vertx vertx) {
        return new CredentialsManagementHttpEndpoint(vertx);
    }

}
