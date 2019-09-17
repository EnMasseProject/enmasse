/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base.infinispan.cache;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ServerConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Index;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;
import io.enmasse.iot.service.base.infinispan.device.DeviceCredential;
import io.enmasse.iot.service.base.infinispan.device.DeviceInformation;
import io.enmasse.iot.service.base.infinispan.device.DeviceKey;

@Component
public class DeviceManagementCacheProvider extends AbstractCacheProvider {

    private static final String GENERATED_PROTOBUF_FILE_NAME = "deviceRegistry.proto";
    private static final Logger log = LoggerFactory.getLogger(DeviceManagementCacheProvider.class);

    @Autowired
    public DeviceManagementCacheProvider(final InfinispanProperties properties) throws Exception {
        super(properties);
    }

    @Override
    protected void customizeServerConfiguration(ServerConfigurationBuilder configuration) {
        configuration.marshaller(ProtoStreamMarshaller.class);
    }

    @Override
    public void start() throws Exception {
       super.start();
       configureSerializer(this.remoteCacheManager);
    }

    private static void configureSerializer(RemoteCacheManager remoteCacheManager) throws Exception {
        final SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);

        final String generatedSchema = new ProtoSchemaBuilder()

                .addClass(DeviceKey.class)
                .addClass(DeviceInformation.class)
                .addClass(DeviceCredential.class)

                .packageName("io.enmasse.iot.registry.infinispan.data")
                .fileName(GENERATED_PROTOBUF_FILE_NAME)
                .build(serCtx);

        log.debug("Generated protobuf schema - {}", generatedSchema);

        remoteCacheManager
                .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME)
                .put(GENERATED_PROTOBUF_FILE_NAME, generatedSchema);

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
