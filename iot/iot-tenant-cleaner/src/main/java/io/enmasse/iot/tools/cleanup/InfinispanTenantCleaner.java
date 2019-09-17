/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import io.enmasse.iot.service.base.infinispan.cache.DeviceConnectionCacheProvider;
import io.enmasse.iot.service.base.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;
import io.enmasse.iot.service.base.infinispan.devcon.DeviceConnectionKey;
import io.enmasse.iot.service.base.infinispan.device.DeviceInformation;
import io.enmasse.iot.service.base.infinispan.device.DeviceKey;
import io.enmasse.iot.service.base.infinispan.tenant.TenantHandle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Expression;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfinispanTenantCleaner {

    private String tenantIdToClean;
    private InfinispanProperties infinispanProperties;
    private int maxQueryResults;

    private RemoteCache<DeviceKey, DeviceInformation> devicesCache;
    private RemoteCache<DeviceConnectionKey, String> devicesConnectionCache;

    DeviceManagementCacheProvider mgmtProvider;
    DeviceConnectionCacheProvider deviceconProvider;

    private final Logger log = LoggerFactory.getLogger(InfinispanTenantCleaner.class);
    private final Vertx vertx;

    private final String pathToConfig;

    public InfinispanTenantCleaner(String pathToConfig) {
        VertxOptions vxOptions = new VertxOptions().setBlockedThreadCheckInterval(200000);
        vertx = Vertx.vertx(vxOptions);
        this.pathToConfig = pathToConfig;
    }

    public Future<Void> run(Future<Void> startPromise) {

        try {
            mgmtProvider = new DeviceManagementCacheProvider(infinispanProperties);
            mgmtProvider.start();
            devicesCache = mgmtProvider.getDeviceManagementCache();

            deviceconProvider = new DeviceConnectionCacheProvider(infinispanProperties);
            deviceconProvider.start();
            devicesConnectionCache = deviceconProvider.getDeviceStateCache();
        } catch (Exception e) {
            log.error("Unable to access Infinispan caches.", e.getCause());
            startPromise.fail(e);
            return startPromise;
        }

        // Query and delete entries in devicesCache
        QueryFactory queryFactory = Search.getQueryFactory(devicesCache);
        QueryBuilder query = queryFactory.from(DeviceInformation.class)
                .select(Expression.property("deviceId"))
                .having("tenantId")
                .eq(tenantIdToClean)
                .maxResults(maxQueryResults);


        List<Object[]> matches;
        do {
            matches = query
                    //.startOffset(loopNumber.getAndIncrement()*maxQueryResults)
                    .build().list();

            matches.forEach(queryResult -> {
                    DeviceKey key = DeviceKey
                            .deviceKey(TenantHandle.of(tenantIdToClean, tenantIdToClean), (String)queryResult[0]);
                    devicesCache.remove(key);

                    DeviceConnectionKey conKey = new DeviceConnectionKey(tenantIdToClean, (String)queryResult[0]);
                    devicesConnectionCache.remove(conKey);
            });
        } while (matches.size() == maxQueryResults);

        stop();
        startPromise.complete();
        return startPromise;
    }

    public void stop() {
        // Stop the cache managers and release all resources
        try {
            deviceconProvider.stop();
            mgmtProvider.stop();
        } catch (Exception n) {
            log.warn("Could not properly release cache manager resources in infinispan");
        }
    }

    public Future<Void> configure() {

        Future<Void> configured = Future.future();

        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("yaml")
                        .setConfig(new JsonObject().put("path", pathToConfig)))
                .addStore(new ConfigStoreOptions()
                        .setType("env")));

        retriever.getConfig(json -> {
            if (json.failed()) {
                log.error("Failed to read configuration.", json.cause());
                configured.fail(json.cause());
            } else {
                CleanerConfigValues config = json.result().mapTo(CleanerConfigValues.class);

                maxQueryResults = config.getDeletionChuckSize();

                String verif = config.verify();
                if (verif != null){
                    log.error(verif);
                    configured.fail(new IllegalArgumentException(String.format(verif)));
                } else {
                    tenantIdToClean = config.getTenantId();
                    infinispanProperties = CleanerConfigValues.createInfinispanProperties(config);
                    configured.complete();
                }
            }
        });
        return configured;
    }
}