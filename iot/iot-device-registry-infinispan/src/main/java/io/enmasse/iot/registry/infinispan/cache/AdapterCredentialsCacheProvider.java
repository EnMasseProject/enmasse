/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.cache;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.infinispan.config.InfinispanProperties;
import io.enmasse.iot.registry.infinispan.device.data.CredentialKey;

@Component
public class AdapterCredentialsCacheProvider extends AbstractCacheProvider {

    @Autowired
    public AdapterCredentialsCacheProvider(final InfinispanProperties properties) throws Exception {
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


    public RemoteCache<CredentialKey, String> getAdapterCredentialsCache() {
        return getOrCreateCache(properties.getAdapterCredentialsCacheName(), buildConfiguration());
    }


}
