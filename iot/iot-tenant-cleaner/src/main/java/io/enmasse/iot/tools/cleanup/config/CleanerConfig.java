/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;

import io.enmasse.iot.infinispan.config.InfinispanProperties;
import io.enmasse.iot.utils.MoreFutures;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CleanerConfig {

    public static final int DEFAULT_DELETION_CHUNK_SIZE = 100;

    private String tenantId;

    private int deletionChunkSize = DEFAULT_DELETION_CHUNK_SIZE;

    private InfinispanProperties infinispan = new InfinispanProperties();

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getDeletionChunkSize() {
        if (this.deletionChunkSize <= 0) {
            return DEFAULT_DELETION_CHUNK_SIZE;
        }
        return this.deletionChunkSize;
    }

    public void setDeletionChunkSize(final int deletionChunkSize) {
        this.deletionChunkSize = deletionChunkSize;
    }

    public void setInfinispan(final InfinispanProperties infinispanProperties) {
        this.infinispan = infinispanProperties;
    }

    public InfinispanProperties getInfinispan() {
        return this.infinispan;
    }

    private static RuntimeException missingField(final String fieldName) {
        return new IllegalArgumentException(String.format("'%s' is missing in configuration", fieldName));
    }

    public CleanerConfig verify() throws RuntimeException {

        final LinkedList<RuntimeException> result = new LinkedList<>();

        if (getTenantId() == null) {
            result.add(missingField("tenantId"));
        }
        if (this.infinispan.getHost().isBlank()) {
            result.add(missingField("infinispan.host"));
        }
        if (this.infinispan.getPort() <= 0) {
            result.add(missingField("infinispan.port"));
        }
        if (this.infinispan.getDevicesCacheName().isBlank()) {
            result.add(missingField("infinispan.devicesCacheName"));
        }
        if (this.infinispan.getDeviceStatesCacheName().isBlank()) {
            result.add(missingField("infinispan.deviceStatesCacheName"));
        }

        // create result

        final RuntimeException e = result.pollFirst();
        if (e != null) {
            result.forEach(e::addSuppressed);
            throw e;
        }

        return this;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tenantId", this.tenantId)
                .add("deletionChunkSize", this.deletionChunkSize)
                .add("infinspanProperties", this.infinispan)
                .toString();
    }


    public static CleanerConfig load(final Optional<Path> pathToConfig) throws Exception {

        final Vertx vertx = Vertx.factory.vertx();

        try {

            var options = new ConfigRetrieverOptions();

            // add path if present

            pathToConfig.ifPresent(path -> {
                options.addStore(new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("yaml")
                        .setConfig(new JsonObject().put("path", path.toAbsolutePath().toString())));
            });

            // add env vars

            options.addStore(new ConfigStoreOptions()
                    .setType("env"));

            // add system properties

            options.addStore(new ConfigStoreOptions()
                    .setType("sys"));

            // create config retriever

            final ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
            retriever.setConfigurationProcessor(HierarchicalProcessor.defaultProcessor());

            // set up futures

            final Future<JsonObject> configured = Future.future();
            retriever.getConfig(configured);

            // fetch config

            var result = configured
                    .map(json -> json.mapTo(CleanerConfig.class))
                    .map(CleanerConfig::verify);

            // return result

            return MoreFutures.map(result).get(30, TimeUnit.SECONDS);

        } finally {
            vertx.close();
        }
    }
}
