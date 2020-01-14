/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.vertx.core.Vertx;

@Component
public class DeviceManagementCacheHealth extends AbstractCacheHealthCheck {

    @Autowired
    public DeviceManagementCacheHealth(final DeviceManagementCacheProvider provider, final Vertx vertx) {
        super("management", provider.getDeviceManagementCache().get(), vertx);
    }

}
