/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import java.io.IOException;

import java.util.UUID;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;

/**
* This is heavily inspired from Tristan Tarrant's SimpleEmbeddedHotRodServer.
* Mimics a remote server using an embedded cache
*
* https://github.com/tristantarrant/infinispan-playground-embedded-hotrod/blob/master/src/main/java/net/dataforte/infinispan/playground/embeddedhotrod/SimpleEmbeddedHotRodServer.java
*/
public class EmbeddedHotRodServer {

    private final RemoteCacheManager manager;
    private final HotRodServer server;
    private final DefaultCacheManager defaultCacheManager;

    private final String CACHE_NAME = UUID.randomUUID().toString();

    public EmbeddedHotRodServer() throws IOException {

        org.infinispan.configuration.cache.ConfigurationBuilder embeddedBuilder = new org.infinispan.configuration.cache.ConfigurationBuilder();
        defaultCacheManager = new DefaultCacheManager(embeddedBuilder.build());
        defaultCacheManager.createCache(CACHE_NAME, embeddedBuilder.build());

        HotRodServerConfiguration build = new HotRodServerConfigurationBuilder().build();
        server = new HotRodServer();
            server.start(build, defaultCacheManager);

        ConfigurationBuilder remoteBuilder = new ConfigurationBuilder()
            .addServers("localhost")
            .marshaller(new ProtoStreamMarshaller());
        manager = new RemoteCacheManager(remoteBuilder.build());

        final SerializationContext serialCtx = ProtoStreamMarshaller.getSerializationContext(manager);

        // genereate the protobuff schema
        String generatedSchema = new ProtoSchemaBuilder()
                    .addClass(RegistryTenantObject.class)
                    .addClass(RegistryCredentialObject.class)
                    .addClass(CredentialsKey.class)
                    .addClass(RegistrationKey.class)
                    .packageName("registry")
                    .fileName("registry.proto")
                    .build(serialCtx);

        // register the schema with the server
        RemoteCache rcache = manager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        rcache.put("registry.proto", generatedSchema);
    }

    public RemoteCache getCache(){
        return manager.getCache(CACHE_NAME);
    }

    public void stop() {
        defaultCacheManager.removeCache(CACHE_NAME);
        manager.stop();
        defaultCacheManager.stop();
        server.stop();
    }
}
