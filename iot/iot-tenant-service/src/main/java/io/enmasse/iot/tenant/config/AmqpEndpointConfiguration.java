/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.utils.ConfigBase;

@Configuration
public class AmqpEndpointConfiguration {

    @Qualifier(Constants.QUALIFIER_AMQP)
    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".amqp")
    public ServiceConfigProperties amqpEndpointProperties() {
        return new ServiceConfigProperties();
    }

}
