/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.cache;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ServerConfigurationBuilder;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.registry.infinispan.config.InfinispanProperties;

public abstract class AbstractCacheProvider implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AbstractCacheProvider.class);

    protected final InfinispanProperties properties;

    protected RemoteCacheManager remoteCacheManager;

    public AbstractCacheProvider(final InfinispanProperties properties) {
        this.properties = properties;
        log.info("Using Infinispan - host: {}, port: {}", properties.getHost(), properties.getPort());
    }

    @PostConstruct
    public void start() throws Exception {

        var config = new ConfigurationBuilder()
                .addServer()
                .host(this.properties.getHost())
                .port(this.properties.getPort());

        if (this.properties.isUseTls()) {
            config.security()
                    .ssl()
                    .enable()
                    .sniHostName(sniHostName(this.properties.getHost()))

                    .trustStorePath(this.properties.getTrustStorePath());
        }

        if (this.properties.getUsername() != null) {
            config
                    .security()
                    .authentication()
                    .realm(this.properties.getSaslRealm())
                    .serverName(this.properties.getSaslServerName())
                    .username(this.properties.getUsername())
                    .password(this.properties.getPassword());
        }

        customizeServerConfiguration(config);

        this.remoteCacheManager = new RemoteCacheManager(config.build());

    }

    protected void customizeServerConfiguration(final ServerConfigurationBuilder configuration) {}

    @PreDestroy
    public void stop() throws Exception {
        this.remoteCacheManager.close();
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    /**
     * Make it a proper SNI hostname.
     * <br>
     * SNI hostnames must, even if fully qualified, not end with a dot.
     *
     * @param host The original hostname
     * @return The SNI compatible hostname
     */
    private static String sniHostName(final String host) {
        return host.replaceAll("\\.$", "");
    }

    protected <K, V> RemoteCache<K, V> getOrCreateCache(final String cacheName, final Configuration configuration) {

        log.debug("CacheConfig - {}\n{}", cacheName, configuration.toXMLString(cacheName));

        if (this.properties.isTryCreate()) {
            return this.remoteCacheManager
                    .administration()
                    .getOrCreateCache(cacheName, configuration);
        } else {
            final RemoteCache<K,V> result = this.remoteCacheManager.getCache(cacheName);
            if (result == null) {
                throw new IllegalStateException(String.format("Cache '%s' not found, and not requested to create", cacheName));
            }
            return result;
        }

    }

    protected <K, V> RemoteCache<K, V> getOrCreateTestCache(final String cacheName, final Configuration configuration) {

        log.debug("CacheConfig - {}\n{}", cacheName, configuration.toXMLString(cacheName));

            return this.remoteCacheManager
                    .administration()
                    .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                    .getOrCreateCache(cacheName, configuration);
    }
}
