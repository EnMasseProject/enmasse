/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.jdbc.config.JdbcProperties;
import io.enmasse.iot.utils.ConfigBase;

@Configuration
public class JdbcConfiguration {

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.jdbc")
    public JdbcProperties jdbcProperties () {
        return new JdbcProperties ();
    }

}
