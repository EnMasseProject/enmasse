/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.cache;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ServerConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Index;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.infinispan.config.InfinispanProperties;
import io.enmasse.iot.registry.infinispan.device.data.DeviceCredential;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;

@Component
public class DeviceManagementCacheProvider extends AbstractCacheProvider {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementCacheProvider.class);

    @Autowired
    public DeviceManagementCacheProvider(final InfinispanProperties properties) throws Exception {
        super(properties);
    }

    @Override
    protected void customizeServerConfiguration(ServerConfigurationBuilder configuration) {
        configuration.addContextInitializer(new DeviceManagementProtobufSchemaBuilderImpl());
    }

    @Override
    public void start() throws Exception {
       super.start();
       configureSerializer(this.remoteCacheManager);
    }

    private static void configureSerializer(RemoteCacheManager remoteCacheManager) throws Exception {

        final DeviceManagementProtobufSchemaBuilderImpl schema = new DeviceManagementProtobufSchemaBuilderImpl();

        remoteCacheManager
                .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME)
                .put(schema.getProtoFileName(), schema.getProtoFile());

    }

    public org.infinispan.configuration.cache.Configuration buildConfiguration() {
        return new org.infinispan.configuration.cache.ConfigurationBuilder()

                .indexing()
                .index(Index.PRIMARY_OWNER)
                .addProperty("default.indexmanager", "org.infinispan.query.indexmanager.InfinispanIndexManager")
                .addIndexedEntity(DeviceInformation.class)
                .addIndexedEntity(DeviceCredential.class)

                .persistence()
                .addSingleFileStore()
                .fetchPersistentState(true)

                .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash()
                .numOwners(1)

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

    public RemoteCache<DeviceKey, DeviceInformation> getDeviceManagementCache() {
        return getOrCreateCache(properties.getDevicesCacheName(), buildConfiguration());
    }

}
