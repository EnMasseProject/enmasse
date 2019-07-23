/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import javax.annotation.PreDestroy;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.infinispan.config.InfinispanProperties;

@Component
public class CacheProvider implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CacheProvider.class);

    private RemoteCacheManager remoteCacheManager;

    @Autowired
    public CacheProvider(final InfinispanProperties properties) {
        logger.info("Using Infinispan - host: {}, port: {}", properties.getHost(), properties.getPort());

        var config = new ConfigurationBuilder()

                .addServer()
                .host(properties.getHost())
                .port(properties.getPort())

                .build();

        this.remoteCacheManager = new RemoteCacheManager(config);
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
       this.remoteCacheManager.close();
    }

    org.infinispan.configuration.cache.Configuration buildConfiguration() {
        return new org.infinispan.configuration.cache.ConfigurationBuilder ()

                .invocationBatching()
                .enable()

                .transaction()
                .autoCommit(true)
                .transactionMode(TransactionMode.TRANSACTIONAL)
                .useSynchronization(true)
                .recovery().disable()

                .locking()
                .isolationLevel(IsolationLevel.READ_COMMITTED)

                .build();
    }

    public <K, V> RemoteCache<K, V> getOrCreateCache(final String cacheName) {
        var configuration = buildConfiguration();

        return this.remoteCacheManager
            .administration()
            .getOrCreateCache(cacheName, configuration);
    }

    public <K, V> RemoteCache<K, V> getCache(final String cacheName) {
        return this.remoteCacheManager.getCache(cacheName);
    }

}
