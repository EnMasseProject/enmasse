/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import static io.enmasse.iot.registry.infinispan.Profiles.PROFILE_DEVICE_CONNECTION;
import static io.vertx.core.Vertx.vertx;

import org.eclipse.hono.deviceregistry.service.deviceconnection.MapBasedDeviceConnectionService;
import org.eclipse.hono.deviceregistry.service.deviceconnection.MapBasedDeviceConnectionsConfigProperties;
import org.eclipse.hono.service.amqp.AmqpEndpoint;
import org.eclipse.hono.service.deviceconnection.DelegatingDeviceConnectionAmqpEndpoint;
import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(PROFILE_DEVICE_CONNECTION)
public class DeviceConnectionServiceConfiguration {

    /**
     * Creates a new instance of an AMQP 1.0 protocol handler for Hono's <em>Device Connection</em> API.
     *
     * @param service The service instance to delegate to.
     * @return The handler.
     */
    @Bean
    @ConditionalOnBean(DeviceConnectionService.class)
    public AmqpEndpoint deviceConnectionAmqpEndpoint(final DeviceConnectionService service) {
        return new DelegatingDeviceConnectionAmqpEndpoint<DeviceConnectionService>(vertx(), service);
    }

    @Bean
    public MapBasedDeviceConnectionsConfigProperties deviceConnectionProperties() {
        return new MapBasedDeviceConnectionsConfigProperties();
    }

    /**
     * Creates an instance of the file based service for managing device connection information.
     *
     * @return The service.
     */
    @Bean
    public DeviceConnectionService deviceConnectionService() {
        final MapBasedDeviceConnectionService service = new MapBasedDeviceConnectionService();
        service.setConfig(deviceConnectionProperties());
        return service;
    }

}
