/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_DEVICE_CONNECTION;
import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_ADAPTER;
import io.enmasse.iot.registry.server.DeviceRegistryAmqpServer;
import io.enmasse.iot.utils.ConfigBase;

@Configuration
public class AmqpEndpointConfiguration {

    /**
     * Gets properties for configuring the Device Registry's AMQP 1.0 endpoint.
     *
     * @return The properties.
     */
    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".amqp")
    @Qualifier(Constants.QUALIFIER_AMQP)
    public ServiceConfigProperties amqpProperties() {
        return new ServiceConfigProperties();
    }

    /**
     * Creates a new server for exposing the device registry's AMQP 1.0 based
     * endpoints.
     *
     * @return The server.
     */
    @Bean
    @Profile({PROFILE_REGISTRY_ADAPTER, PROFILE_DEVICE_CONNECTION})
    public DeviceRegistryAmqpServer amqpServer() {
        return new DeviceRegistryAmqpServer();
    }

}
