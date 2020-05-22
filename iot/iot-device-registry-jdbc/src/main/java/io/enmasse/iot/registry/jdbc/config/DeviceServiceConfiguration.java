/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_ADAPTER;
import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_MANAGEMENT;
import static io.vertx.core.Vertx.vertx;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.service.amqp.AmqpEndpoint;
import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.service.credentials.DelegatingCredentialsAmqpEndpoint;
import org.eclipse.hono.service.http.HttpEndpoint;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.credentials.DelegatingCredentialsManagementHttpEndpoint;
import org.eclipse.hono.service.management.device.DelegatingDeviceManagementHttpEndpoint;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.registration.DelegatingRegistrationAmqpEndpoint;
import org.eclipse.hono.service.registration.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.enmasse.iot.jdbc.store.device.AbstractDeviceAdapterStore;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceManagementStore;
import io.enmasse.iot.registry.jdbc.device.impl.CredentialsManagementServiceImpl;
import io.enmasse.iot.registry.jdbc.device.impl.CredentialsServiceImpl;
import io.enmasse.iot.registry.jdbc.device.impl.DeviceManagementServiceImpl;
import io.enmasse.iot.registry.jdbc.device.impl.RegistrationServiceImpl;
import io.enmasse.iot.registry.tenant.KubernetesTenantInformationService;
import io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler;
import io.enmasse.iot.registry.util.DeviceRegistryTokenAuthProvider;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.handler.AuthHandler;

@Configuration
@Profile({PROFILE_REGISTRY_ADAPTER, PROFILE_REGISTRY_MANAGEMENT})
public class DeviceServiceConfiguration {

    /**
     * Exposes the JDBC registration service as a Spring bean.
     *
     * @param store The JDBC store.
     * @return The JDBC registration service.
     */
    @Bean
    @Profile(PROFILE_REGISTRY_ADAPTER)
    public RegistrationService registrationService(AbstractDeviceAdapterStore store) {
        return new RegistrationServiceImpl(store);
    }

    /**
     * Exposes the JDBC credentials service as a Spring bean.
     *
     * @param store The JDBC store.
     * @return The JDBC credentials service.
     */
    @Bean
    @Profile(PROFILE_REGISTRY_ADAPTER)
    public CredentialsService credentialsService(AbstractDeviceAdapterStore store) {
        return new CredentialsServiceImpl(store);
    }

    /**
     * Exposes JDBC device management service as a Spring bean
     *
     * @param store The JDBC store.
     * @param properties The service properties.
     * @return The JDBC device management service
     */
    @Bean
    @Profile(PROFILE_REGISTRY_MANAGEMENT)
    public DeviceManagementService deviceManagementService(final AbstractDeviceManagementStore store, final DeviceServiceProperties properties) {
        return new DeviceManagementServiceImpl(store, properties);
    }

    /**
     * Exposes JDBC credential management service as a Spring bean
     *
     * @param vertx The Verx instance.
     * @param store The JDBC store
     * @param properties The service properties.
     * @return The JDBC credential management service
     */
    @Bean
    @Profile(PROFILE_REGISTRY_MANAGEMENT)
    public CredentialsManagementService credentialsManagementService(final Vertx vertx, HonoPasswordEncoder passwordEncoder, final AbstractDeviceManagementStore store, final DeviceServiceProperties properties) {
        return new CredentialsManagementServiceImpl(vertx, passwordEncoder, store, properties);
    }

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Device Registration</em> API.
     *
     * @param service The service instance to delegate to.
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(RegistrationService.class)
    public AmqpEndpoint registrationAmqpEndpoint(final RegistrationService service) {
        return new DelegatingRegistrationAmqpEndpoint<>(vertx(), service);
    }

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Credentials</em> API.
     *
     * @param service The service instance to delegate to.
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(CredentialsService.class)
    public AmqpEndpoint credentialsAmqpEndpoint(final CredentialsService service) {
        return new DelegatingCredentialsAmqpEndpoint<>(vertx(), service);
    }

    /**
     * Creates a new instance of an HTTP protocol handler for the <em>devices</em> resources
     * of Hono's Device Registry Management API's.
     *
     * @param vertx The vert.x instance to run on.
     * @param service The service instance to delegate to.
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(DeviceManagementService.class)
    public HttpEndpoint deviceHttpEndpoint(final Vertx vertx, final DeviceManagementService service) {
        return new DelegatingDeviceManagementHttpEndpoint<>(vertx, service);
    }

    /**
     * Creates a new instance of an HTTP protocol handler for the <em>credentials</em> resources
     * of Hono's Device Registry Management API's.
     *
     * @param vertx The vert.x instance to run on.
     * @param service The service instance to delegate to.
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(CredentialsManagementService.class)
    public HttpEndpoint credentialsHttpEndpoint(final Vertx vertx, final CredentialsManagementService service) {
        return new DelegatingCredentialsManagementHttpEndpoint<>(vertx, service);
    }

    /**
     * Creates an authentication handler used by device registry management HTTP API.
     *
     * @return The handler.
     */
    @Autowired
    @Bean
    public AuthHandler authHandler(final Tracer tracer, final RestEndpointConfiguration restEndpointConfiguration) {
        return new DeviceRegistryTokenAuthHandler(tracer, authProvider(tracer, restEndpointConfiguration));
    }

    @Autowired
    @Bean
    public AuthProvider authProvider(final Tracer tracer, final RestEndpointConfiguration restEndpointConfiguration) {
        return new DeviceRegistryTokenAuthProvider(tracer, restEndpointConfiguration.getAuthTokenCacheExpiration());
    }

    /**
     * Exposes a password encoder to use for encoding clear text passwords
     * and for matching password hashes.
     *
     * @return The encoder.
     */
    @Autowired
    @Bean
    @Profile(PROFILE_REGISTRY_MANAGEMENT)
    public HonoPasswordEncoder passwordEncoder(final DeviceServiceProperties deviceServiceProperties) {
        return new SpringBasedHonoPasswordEncoder(deviceServiceProperties.getMaxBcryptIterations());
    }

    @Bean
    public KubernetesTenantInformationService tenantInformationService() {
        return new KubernetesTenantInformationService();
    }

}
