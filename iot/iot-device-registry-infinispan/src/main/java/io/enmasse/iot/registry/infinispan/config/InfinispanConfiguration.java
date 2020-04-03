/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import static io.enmasse.iot.registry.infinispan.Profiles.PROFILE_DEVICE_CONNECTION;
import static io.enmasse.iot.registry.infinispan.Profiles.PROFILE_DEVICE_REGISTRY;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.enmasse.iot.infinispan.config.InfinispanProperties;
import io.enmasse.iot.utils.ConfigBase;

@Configuration
public class InfinispanConfiguration {

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".device-connection.infinispan")
    @Profile(PROFILE_DEVICE_CONNECTION)
    public InfinispanProperties infinispanPropertiesConnection () {
        return new InfinispanProperties ();
    }

    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.infinispan")
    @Profile(PROFILE_DEVICE_REGISTRY)
    public InfinispanProperties infinispanPropertiesRegistry () {
        return new InfinispanProperties ();
    }

}
