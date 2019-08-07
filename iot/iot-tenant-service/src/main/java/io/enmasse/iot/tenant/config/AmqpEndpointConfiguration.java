/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.tenant.TenantAmqpEndpoint;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.vertx.core.Vertx;

@Configuration
public class AmqpEndpointConfiguration {

    @Bean
    public TenantAmqpEndpoint tenantAmqpEndpoint(@Autowired final Vertx vertx) {
        return new TenantAmqpEndpoint(vertx);
    }

    @Qualifier(Constants.QUALIFIER_AMQP)
    @Bean
    @ConfigurationProperties(prefix = "enmasse.iot.tenant.endpoint.amqp")
    public ServiceConfigProperties amqpEndpointProperties() {
        return new ServiceConfigProperties();
    }

}
