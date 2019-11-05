/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan.cache;

import java.util.Optional;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.config.InfinispanProperties;
import io.enmasse.iot.infinispan.devcon.DeviceConnectionKey;

@Component
public class DeviceConnectionCacheProvider extends AbstractCacheProvider {

    @Autowired
    public DeviceConnectionCacheProvider(final InfinispanProperties properties) {
        super(properties);
    }

    public org.infinispan.configuration.cache.Configuration buildConfiguration() {
        return new org.infinispan.configuration.cache.ConfigurationBuilder()

                .indexing()
                .index(Index.NONE)

                .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash()
                .numOwners(1)

                .build();
    }

    public RemoteCache<DeviceConnectionKey, String> getOrCreateDeviceStateCache() {
        return getOrCreateCache(properties.getDeviceStatesCacheName(), this::buildConfiguration);
    }

    public Optional<RemoteCache<DeviceConnectionKey, String>> getDeviceStateCache() {
        return getCache(properties.getDeviceStatesCacheName());
    }

}
