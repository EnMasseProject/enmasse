/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Expression;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.iot.service.base.infinispan.cache.DeviceConnectionCacheProvider;
import io.enmasse.iot.service.base.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.service.base.infinispan.devcon.DeviceConnectionKey;
import io.enmasse.iot.service.base.infinispan.device.DeviceInformation;
import io.enmasse.iot.service.base.infinispan.device.DeviceKey;
import io.enmasse.iot.service.base.infinispan.tenant.TenantHandle;
import io.enmasse.iot.service.base.utils.MoreFutures;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class InfinispanTenantCleaner implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(InfinispanTenantCleaner.class);

    private final Vertx vertx;
    private final Optional<Path> pathToConfig;

    private RemoteCache<DeviceKey, DeviceInformation> devicesCache;
    private RemoteCache<DeviceConnectionKey, String> devicesConnectionCache;

    private DeviceManagementCacheProvider mgmtProvider;
    private DeviceConnectionCacheProvider deviceconProvider;

    public InfinispanTenantCleaner(final Optional<Path> pathToConfig) {
        Objects.requireNonNull(pathToConfig);
        this.vertx = Vertx.vertx();
        this.pathToConfig = pathToConfig;
    }

    /**
     * Execute deleting the tenant information.
     * <br>
     * The method most only return successfully if all tenant information
     * has been erased from the system. Otherwise it must throw an exception.
     *
     * @throws Exception In case anything went wrong.
     */
    public void run() throws Exception {

        var config = configure().get(30, TimeUnit.SECONDS);
        var infinispanProperties = config.createInfinispanProperties();

        this.mgmtProvider = new DeviceManagementCacheProvider(infinispanProperties);
        this.mgmtProvider.start();
        this.devicesCache = mgmtProvider.getDeviceManagementCache();

        this.deviceconProvider = new DeviceConnectionCacheProvider(infinispanProperties);
        this.deviceconProvider.start();
        this.devicesConnectionCache = deviceconProvider.getDeviceStateCache();

        final String tenantIdToClean = config.getTenantId();

        // Query and delete entries in devicesCache
        final QueryFactory queryFactory = Search.getQueryFactory(this.devicesCache);
        final QueryBuilder query = queryFactory.from(DeviceInformation.class)
                .select(Expression.property("deviceId"))
                .having("tenantId")
                .eq(tenantIdToClean)
                .maxResults(config.getDeletionChuckSize());

        log.info("Start deleting tenant information: {}", config);

        List<Object[]> matches;
        do {
            matches = query.build().list();

            matches.forEach(queryResult -> {

                final String deviceId = (String) queryResult[0];

                // delete device connection entry first

                final DeviceConnectionKey conKey = new DeviceConnectionKey(tenantIdToClean, deviceId);
                this.devicesConnectionCache.remove(conKey);

                // delete device registration second

                final DeviceKey regKey = DeviceKey
                        .deviceKey(TenantHandle.of(tenantIdToClean, tenantIdToClean), deviceId);
                this.devicesCache.remove(regKey);

            });

            log.info("Deleted {} entries", matches.size());

        } while (matches.size() > 0);

        log.info("Removed tenant from system");
    }

    public void close() throws Exception {
        // Stop the cache managers and release all resources
        if (this.deviceconProvider != null) {
            this.deviceconProvider.stop();
        }
        if (this.mgmtProvider != null) {
            this.mgmtProvider.stop();
        }
        if (this.vertx != null) {
            this.vertx.close();
        }
    }

    private CompletableFuture<CleanerConfigValues> configure() {

        var options = new ConfigRetrieverOptions();

        // add path if present

        this.pathToConfig.ifPresent(path -> {
            options.addStore(new ConfigStoreOptions()
                    .setType("file")
                    .setFormat("yaml")
                    .setConfig(new JsonObject().put("path", path.toAbsolutePath().toString()))
                    .setOptional(true));
        });

        // add env vars

        options.addStore(new ConfigStoreOptions()
                .setType("env"));

        // create config retriever

        final ConfigRetriever retriever = ConfigRetriever.create(this.vertx, options);

        // set up futures

        final Future<JsonObject> configured = Future.future();
        retriever.getConfig(configured);

        // fetch config

        var result = configured
                .map(json -> json.mapTo(CleanerConfigValues.class))
                .map(CleanerConfigValues::verify);

        // return result

        return MoreFutures.map(result);
    }
}
