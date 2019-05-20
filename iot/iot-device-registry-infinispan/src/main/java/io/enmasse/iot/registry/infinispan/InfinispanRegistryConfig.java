/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import java.util.UUID;
import org.eclipse.hono.deviceregistry.ApplicationConfig;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Spring Boot configuration for the Device Registry application.
 *
 */
@Configuration
public class InfinispanRegistryConfig extends ApplicationConfig {

    /**
     * Connects to an infinispan server and create a randomly named RemoteCache.
     * The constructor will use the hotrod-client.properties file that must be in the classpath.
     *
     * @throws IOException if the Protobuf spec file cannot be created.
     * @return an RemoteCacheManager bean.
     */
    @Bean
    public <K, V> RemoteCache<K,V> getCache() throws IOException {

        final RemoteCacheManager remoteCacheManager = new RemoteCacheManager();
        final SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);

        // genereate the protobuff schema
        String generatedSchema = new ProtoSchemaBuilder()
                .addClass(RegistryTenantObject.class)
                .addClass(RegistryCredentialObject.class)
                .addClass(CredentialsKey.class)
                .addClass(RegistrationKey.class)
                .packageName("registry")
                .fileName("registry.proto")
                .build(serCtx);

        // register the schema with the server
        remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME)
            .put("registry.proto", generatedSchema);

        final String cacheName = UUID.randomUUID().toString();
        return remoteCacheManager.administration().createCache(cacheName, new ConfigurationBuilder().build());
    }
}
