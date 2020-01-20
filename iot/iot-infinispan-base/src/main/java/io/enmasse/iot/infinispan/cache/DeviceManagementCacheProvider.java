/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan.cache;

import java.io.IOException;
import java.util.Optional;

import org.infinispan.client.hotrod.RemoteCache;
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

import io.enmasse.iot.infinispan.config.InfinispanProperties;
import io.enmasse.iot.infinispan.device.CredentialKey;
import io.enmasse.iot.infinispan.device.DeviceCredential;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.infinispan.device.DeviceKey;

@Component
public class DeviceManagementCacheProvider extends AbstractCacheProvider {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementCacheProvider.class);
    private static final String GENERATED_PROTOBUF_FILE_NAME = "deviceRegistry.proto";
    private String schema;

    @Autowired
    public DeviceManagementCacheProvider(final InfinispanProperties properties) {
        super(properties);
    }

    @Override
    protected void customizeServerConfiguration(ServerConfigurationBuilder configuration) {
        configuration.marshaller(ProtoStreamMarshaller.class);
    }

    @Override
    public void start() throws Exception {
        super.start();
        this.schema = generateSchema();
        uploadSchema();
    }

    private void uploadSchema() throws Exception {
        uploadSchema(GENERATED_PROTOBUF_FILE_NAME, this.schema);
    }

    private String generateSchema() throws IOException {
        final SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(this.remoteCacheManager);

        final String generatedSchema = new ProtoSchemaBuilder()

                .addClass(CredentialKey.class)
                .addClass(DeviceKey.class)
                .addClass(DeviceInformation.class)
                .addClass(DeviceCredential.class)

                .packageName(DeviceInformation.class.getPackageName())
                .fileName(GENERATED_PROTOBUF_FILE_NAME)
                .build(serCtx);

        return generatedSchema;
    }

    public org.infinispan.configuration.cache.Configuration buildDeviceManagementConfiguration() {
        return new org.infinispan.configuration.cache.ConfigurationBuilder()

                .indexing()
                .index(Index.PRIMARY_OWNER)
                .addProperty("default.indexmanager", "org.infinispan.query.indexmanager.InfinispanIndexManager")
                .addProperty("default.worker.execution", "async")
                .addProperty("default.index_flush_interval", "500")
                .addIndexedEntity(DeviceInformation.class)
                .addIndexedEntity(DeviceCredential.class)

                // .persistence()
                // .addSingleFileStore()
                // .fetchPersistentState(true)

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

    public RemoteCache<DeviceKey, DeviceInformation> getOrCreateDeviceManagementCache() {
        return getOrCreateCache(properties.getDevicesCacheName(), this::buildDeviceManagementConfiguration);
    }

    public Optional<RemoteCache<DeviceKey, DeviceInformation>> getDeviceManagementCache() {
        return getCache(properties.getDevicesCacheName());
    }

    public org.infinispan.configuration.cache.Configuration buildAdapterCredentialsConfiguration() {
        return new org.infinispan.configuration.cache.ConfigurationBuilder()

                .indexing()
                .index(Index.NONE)

                .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash()
                .numOwners(1)

                .build();
    }

    public RemoteCache<CredentialKey, String> getOrCreateAdapterCredentialsCache() {
        return getOrCreateCache(properties.getAdapterCredentialsCacheName(), this::buildAdapterCredentialsConfiguration);
    }

    public Optional<RemoteCache<CredentialKey, String>> getAdapterCredentialsCache() {
        return getCache(properties.getAdapterCredentialsCacheName());
    }

    public void checkSchema() {
        if (!this.properties.isUploadSchema()) {
            return;
        }

        final Object schema = this.remoteCacheManager
                .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME)
                .get(GENERATED_PROTOBUF_FILE_NAME);

        if (schema == null) {
            throw new IllegalStateException("Schema is missing");
        }
        if (!(schema instanceof String)) {
            throw new IllegalStateException("Schema has illegal content");
        }
        if (!schema.equals(this.schema)) {
            log.info("Schema doesn't match expected content: {} vs {}", this.schema, schema);
            if ( this.properties.isFailOnSchemaMismatch()) {
                throw new IllegalStateException("Schema doesn't match expected content");
            }
        }
    }

}
