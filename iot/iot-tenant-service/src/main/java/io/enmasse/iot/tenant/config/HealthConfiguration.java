/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import io.enmasse.iot.tenant.config.compat.ServerConfig;
import io.enmasse.iot.utils.ConfigBase;
import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = ConfigBase.CONFIG_BASE + ".health-check", namingStrategy = ConfigProperties.NamingStrategy.VERBATIM, failOnMismatchingMember = false)
public class HealthConfiguration extends ServerConfig {

    public org.eclipse.hono.config.ServerConfig toHono() {
        var result = new org.eclipse.hono.config.ServerConfig();
        super.applyTo(result);
        return result;
    }

}
