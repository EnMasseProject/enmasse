/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import java.time.Duration;

import io.enmasse.iot.utils.ConfigBase;
import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = ConfigBase.CONFIG_BASE + ".tenant.service", namingStrategy = ConfigProperties.NamingStrategy.VERBATIM, failOnMismatchingMember = false)
public class TenantServiceConfigProperties {

    private static final Duration DEFAULT_CACHE_TIME_TO_LIVE = Duration.ofMinutes(5);

    private Duration cacheTimeToLive = DEFAULT_CACHE_TIME_TO_LIVE;

    public void setCacheTimeToLive(final Duration cacheTimeToLive) {
        this.cacheTimeToLive = cacheTimeToLive != null ? cacheTimeToLive : DEFAULT_CACHE_TIME_TO_LIVE;
    }

    public Duration getCacheTimeToLive() {
        return this.cacheTimeToLive;
    }
}
