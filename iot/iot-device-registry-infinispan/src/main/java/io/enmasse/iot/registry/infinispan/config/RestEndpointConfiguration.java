/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.util.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.utils.ConfigBase;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class RestEndpointConfiguration {

    /**
     * Defines expiration lifespan for caching authentication tokens in seconds.
     * Default value of 0 means that caching is disabled
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration authTokenCacheExpiration = Duration.ofSeconds(60);

    /**
     * Gets properties for configuring the Device Registry's REST endpoint.
     *
     * @return The properties.
     */
    @Qualifier(Constants.QUALIFIER_REST)
    @Bean
    @ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.rest")
    public ServiceConfigProperties restProperties() {
        final ServiceConfigProperties props = new ServiceConfigProperties();
        return props;
    }

    public Duration getAuthTokenCacheExpiration() {
        return authTokenCacheExpiration;
    }

    public void setAuthTokenCacheExpiration(Duration authTokenCacheExpiration) {
        this.authTokenCacheExpiration = authTokenCacheExpiration;
    }
}
