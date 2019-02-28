/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.tenant.impl.TenantServiceConfigProperties;

@Configuration
public class ServiceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "enmasse.iot.tenant.service")
    public TenantServiceConfigProperties tenantsProperties() {
        return new TenantServiceConfigProperties();
    }
}
