/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.tenant.impl.TenantServiceConfigProperties;
import io.enmasse.iot.utils.ConfigBase;

@Configuration
public class ServiceConfiguration {

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".tenant.service")
    public TenantServiceConfigProperties tenantsProperties() {
        return new TenantServiceConfigProperties();
    }
}
