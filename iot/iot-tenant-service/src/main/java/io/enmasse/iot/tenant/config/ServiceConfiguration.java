/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.eclipse.hono.service.amqp.AmqpEndpoint;
import org.eclipse.hono.service.tenant.DelegatingTenantAmqpEndpoint;
import org.eclipse.hono.service.tenant.TenantService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.tenant.impl.TenantServiceConfigProperties;
import io.enmasse.iot.tenant.impl.TenantServiceImpl;
import io.enmasse.iot.utils.ConfigBase;
import static io.vertx.core.Vertx.vertx;

@Configuration
public class ServiceConfiguration {

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".tenant.service")
    public TenantServiceConfigProperties tenantsProperties() {
        return new TenantServiceConfigProperties();
    }

    @Bean
    public AmqpEndpoint tenantAmqpEndpoint(final TenantService service) {
        return new DelegatingTenantAmqpEndpoint<TenantService>(vertx(), service);
    }

    @Bean
    public TenantServiceImpl tenantService() {
        return new TenantServiceImpl();
    }

}
