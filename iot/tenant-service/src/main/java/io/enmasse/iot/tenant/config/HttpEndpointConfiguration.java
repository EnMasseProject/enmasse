/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.tenant.TenantHttpEndpoint;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import io.enmasse.iot.tenant.impl.TenantRestService;
import io.vertx.core.Vertx;

@Configuration
public class HttpEndpointConfiguration {

    private static final String BEAN_NAME_TENANT_HTTP_SERVICE = "tenantHttpService";

    @Bean
    @Scope("prototype")
    public TenantHttpEndpoint tenantHttpEndpoint(@Autowired final Vertx vertx) {
        return new TenantHttpEndpoint(vertx);
    }

    @Qualifier(Constants.QUALIFIER_REST)
    @Bean
    @ConfigurationProperties(prefix = "enmasse.iot.tenant.endpoint.http")
    public ServiceConfigProperties restProperties() {
        return new ServiceConfigProperties();
    }

    @Bean(BEAN_NAME_TENANT_HTTP_SERVICE)
    @Scope("prototype")
    public TenantRestService deviceRegistryRestServer() {
        return new TenantRestService();
    }

    @Bean
    public ObjectFactoryCreatingFactoryBean deviceRegistryRestServerFactory() {
        final ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
        factory.setTargetBeanName(BEAN_NAME_TENANT_HTTP_SERVICE);
        return factory;
    }
}
