/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import java.io.IOException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;

import io.enmasse.iot.registry.infinispan.config.InfinispanProperties;

/**
 * This is heavily inspired from Tristan Tarrant's SimpleEmbeddedHotRodServer.
 * Mimics a remote server using an embedded cache
 *
 * https://github.com/tristantarrant/infinispan-playground-embedded-hotrod/blob/master/src/main/java/net/dataforte/infinispan/playground/embeddedhotrod/SimpleEmbeddedHotRodServer.java
 */
public class EmbeddedHotRodServer {

    private final HotRodServer server;
    private final DefaultCacheManager defaultCacheManager;
    private CacheProvider provider;

    public EmbeddedHotRodServer() throws IOException {

        var config = new org.infinispan.configuration.cache.ConfigurationBuilder()
                .build();
        defaultCacheManager = new DefaultCacheManager(config);

        final HotRodServerConfiguration build = new HotRodServerConfigurationBuilder()
                .build();
        server = new HotRodServer();
        server.start(build, defaultCacheManager);

        final InfinispanProperties properties = new InfinispanProperties();
        this.provider = new CacheProvider(properties);
    }

    public <K, V> RemoteCache<K, V> getCache(String cacheName) {
        var config = provider.buildConfiguration();

        defaultCacheManager.defineConfiguration(cacheName, config);
        return provider.getCache(cacheName);
    }

    public void stop() throws Exception {
        try {
            provider.close();
        } finally {
            defaultCacheManager.stop();
            server.stop();
        }

    }
}
