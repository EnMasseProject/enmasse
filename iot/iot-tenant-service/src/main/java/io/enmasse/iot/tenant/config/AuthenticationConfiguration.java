/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import io.enmasse.iot.tenant.config.compat.AuthenticationServerClientConfig;
import io.enmasse.iot.utils.ConfigBase;
import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = ConfigBase.CONFIG_BASE + ".auth", namingStrategy = ConfigProperties.NamingStrategy.VERBATIM, failOnMismatchingMember = false)
public class AuthenticationConfiguration extends AuthenticationServerClientConfig {
}
