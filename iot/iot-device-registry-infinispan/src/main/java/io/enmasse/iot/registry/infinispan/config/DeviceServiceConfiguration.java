/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import static io.enmasse.iot.registry.infinispan.Profiles.PROFILE_DEVICE_REGISTRY;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.service.credentials.CredentialsAmqpEndpoint;
import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.service.management.credentials.CredentialsManagementHttpEndpoint;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.device.DeviceManagementHttpEndpoint;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.registration.RegistrationAmqpEndpoint;
import org.eclipse.hono.service.registration.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler;
import io.enmasse.iot.registry.util.DeviceRegistryTokenAuthProvider;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.handler.AuthHandler;

@Configuration
public class DeviceServiceConfiguration {

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Device Registration</em> API.
     *
     * @return The handler.
     */
    @Autowired
    @Bean
    @ConditionalOnBean(RegistrationService.class)
    public RegistrationAmqpEndpoint registrationAmqpEndpoint(final Vertx vertx) {
        return new RegistrationAmqpEndpoint(vertx);
    }

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Credentials</em> API.
     *
     * @return The handler.
     */
    @Autowired
    @Bean
    @ConditionalOnBean(CredentialsService.class)
    public CredentialsAmqpEndpoint credentialsAmqpEndpoint(final Vertx vertx) {
        return new CredentialsAmqpEndpoint(vertx);
    }

    /**
     * Creates a new instance of an HTTP protocol handler for Hono's <em>Device Registration</em> API.
     *
     * @return The handler.
     */
    @Autowired
    @Bean
    @ConditionalOnBean(DeviceManagementService.class)
    public DeviceManagementHttpEndpoint registrationHttpEndpoint(final Vertx vertx) {
        return new DeviceManagementHttpEndpoint(vertx);
    }

    /**
     * Creates a new instance of an HTTP protocol handler for Hono's <em>Credentials</em> API.
     *
     * @return The handler.
     */
    @Autowired
    @Bean
    @ConditionalOnBean(CredentialsManagementService.class)
    public CredentialsManagementHttpEndpoint credentialsHttpEndpoint(final Vertx vertx) {
        return new CredentialsManagementHttpEndpoint(vertx);
    }

    /**
     * Creates an authentication handler used by device registry management HTTP API.
     *
     * @return The handler.
     */
    @Bean
    @Autowired
    public AuthHandler authHandler(final Tracer tracer, final RestEndpointConfiguration restEndpointConfiguration) {
        return new DeviceRegistryTokenAuthHandler(tracer, authProvider(tracer, restEndpointConfiguration));
    }

    @Bean
    @Autowired
    public AuthProvider authProvider(final Tracer tracer, final RestEndpointConfiguration restEndpointConfiguration) {
        return new DeviceRegistryTokenAuthProvider(tracer, restEndpointConfiguration.getAuthTokenCacheExpiration());
    }

    /**
     * Exposes a password encoder to use for encoding clear text passwords
     * and for matching password hashes.
     *
     * @return The encoder.
     */
    @Bean
    @Autowired
    @Profile(PROFILE_DEVICE_REGISTRY)
    public HonoPasswordEncoder passwordEncoder(DeviceServiceProperties deviceServiceProperties) {
        return new SpringBasedHonoPasswordEncoder(deviceServiceProperties.getMaxBcryptIterations());
    }

}
