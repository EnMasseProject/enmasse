/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.infinispan.config.InfinispanProperties;
import static io.enmasse.iot.registry.infinispan.Profiles.PROFILE_DEVICE_REGISTRY;
import io.enmasse.iot.registry.infinispan.device.impl.CredentialsManagementServiceImpl;
import io.enmasse.iot.registry.infinispan.device.impl.CredentialsServiceImpl;
import io.enmasse.iot.registry.infinispan.device.impl.DeviceManagementServiceImpl;
import io.enmasse.iot.registry.infinispan.device.impl.RegistrationServiceImpl;
import io.enmasse.iot.registry.tenant.KubernetesTenantInformationService;
import io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler;
import io.enmasse.iot.registry.util.DeviceRegistryTokenAuthProvider;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import static io.vertx.core.Vertx.vertx;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.handler.AuthHandler;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.auth.SpringBasedHonoPasswordEncoder;
import org.eclipse.hono.deviceregistry.service.tenant.TenantInformationService;
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
import org.springframework.context.annotation.Scope;

@Configuration
@Profile({PROFILE_DEVICE_REGISTRY})
public class DeviceServiceConfiguration {

    @Bean
    public DeviceManagementCacheProvider deviceManagementCacheProvider(final InfinispanProperties infinispanProperties) {
        return new DeviceManagementCacheProvider(infinispanProperties);
    }

    /**
     * Exposes the Infinispan registration service as a Spring bean.
     *
     * @return The Infinispan registration service.
     */
    @Bean
    public RegistrationService registrationService(final DeviceManagementCacheProvider cacheProvider) {
        return new RegistrationServiceImpl(cacheProvider);
    }

    /**
     * Exposes the Infinispan credentials service as a Spring bean.
     *
     * @return The Infinispan credentials service.
     */
    @Bean
    public CredentialsService credentialsService(final DeviceManagementCacheProvider cacheProvider, final DeviceServiceProperties properties) {
        return new CredentialsServiceImpl(cacheProvider, properties);
    }

    /**
     * Exposes Infinispan device management service as a Spring bean
     *
     * @param cacheProvider The cache provider.
     * @return The Infinispan device management service
     */
    @Bean
    public DeviceManagementService deviceManagementService(final DeviceManagementCacheProvider cacheProvider) {
        return new DeviceManagementServiceImpl(cacheProvider);
    }

    /**
     * Exposes Infinispan credential management service as a Spring bean
     *
     * @param cacheProvider The cache provider.
     * @return The Infinispan credential management service
     */
    @Bean
    public CredentialsManagementService credentialsManagementService(final Vertx vertx, HonoPasswordEncoder passwordEncoder, final DeviceManagementCacheProvider cacheProvider) {
        return new CredentialsManagementServiceImpl(vertx, passwordEncoder, cacheProvider);
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
        return new DelegatingRegistrationAmqpEndpoint<RegistrationService>(vertx(), service);
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
        return new DelegatingCredentialsAmqpEndpoint<CredentialsService>(vertx(), service);
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
        return new DelegatingDeviceManagementHttpEndpoint<DeviceManagementService>(vertx, service);
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
        return new DelegatingCredentialsManagementHttpEndpoint<CredentialsManagementService>(vertx, service);
    }

    /**
     * Creates an authentication handler used by device registry management HTTP API.
     *
     * @return The handler.
     */
    @Bean
    public AuthHandler authHandler(final Tracer tracer, final RestEndpointConfiguration restEndpointConfiguration) {
        return new DeviceRegistryTokenAuthHandler(tracer, authProvider(tracer, restEndpointConfiguration));
    }

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
    @Bean
    @Profile(PROFILE_DEVICE_REGISTRY)
    public HonoPasswordEncoder passwordEncoder(DeviceServiceProperties deviceServiceProperties) {
        return new SpringBasedHonoPasswordEncoder(deviceServiceProperties.getMaxBcryptIterations());
    }

    @Bean
    public KubernetesTenantInformationService tenantInformationService() {
        return new KubernetesTenantInformationService();
    }

}
