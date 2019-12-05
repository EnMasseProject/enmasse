/*
 *  Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.IndexedQueryMode;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.enmasse.iot.infinispan.cache.DeviceConnectionCacheProvider;
import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.infinispan.devcon.DeviceConnectionKey;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.infinispan.device.DeviceKey;
import io.enmasse.iot.infinispan.tenant.TenantHandle;
import io.enmasse.iot.tools.cleanup.config.CleanerConfig;

public class InfinispanTenantCleaner implements AutoCloseable {

    private interface Closer {
        void close() throws Exception;
    }

    private static final Logger log = LoggerFactory.getLogger(InfinispanTenantCleaner.class);

    private CleanerConfig config;

    public InfinispanTenantCleaner(final CleanerConfig config) {
        this.config = config;
    }

    @Override
    public void close() throws Exception {}

    /**
     * Execute deleting the tenant information.
     * <br>
     * The method most only return successfully if all tenant information
     * has been erased from the system. Otherwise it must throw an exception.
     *
     * @throws Exception In case anything went wrong.
     */
    public void run() throws Exception {

        final LinkedList<Closer> cleanup = new LinkedList<>();

        try (
                var mgmtProvider = new DeviceManagementCacheProvider(config.getInfinispan());
                var deviceconProvider = new DeviceConnectionCacheProvider(config.getInfinispan());) {

            mgmtProvider.start();
            cleanup.add(mgmtProvider::stop);
            final var devicesCache = mgmtProvider.getOrCreateDeviceManagementCache();
            cleanup.add(devicesCache::stop);

            deviceconProvider.start();
            cleanup.add(deviceconProvider::stop);
            final var devicesConnectionCache = deviceconProvider.getDeviceStateCache().orElse(null);
            cleanup.add(devicesConnectionCache::stop);

            performCleanup(config, devicesCache, devicesConnectionCache);

        } finally {

            for (final Closer c : Lists.reverse(cleanup)) {
                try {
                    c.close();
                } catch (Exception e) {
                }
            }

        }
    }

    private static <T> T measure(final String operation, final Supplier<T> s) {
        final Instant start = Instant.now();
        final T result = s.get();
        final Duration duration = Duration.between(start, Instant.now());
        log.debug("{} - {}", operation, duration);
        return result;
    }

    private void performCleanup(
            final CleanerConfig config,
            final RemoteCache<DeviceKey, DeviceInformation> devicesCache,
            final RemoteCache<DeviceConnectionKey, String> devicesConnectionCache) {

        final String tenantId = config.getTenantId();

        // Query and delete entries in devicesCache
        final Query query = createQuery(config, devicesCache, tenantId);

        log.info("Start deleting tenant data: {}", config);

        long count = 0;
        int len;
        long remaining;
        do {
            final List<DeviceInformation> result = measure("List", () -> query.list());
            log.debug("List: {}", result);
            measure("Delete", () -> {
                result.forEach(entry -> {
                    log.debug("result: {}", entry);
                    deleteDevice(devicesCache, devicesConnectionCache, tenantId, entry.getDeviceId());
                });
                return null;
            });

            len = result.size();
            count += len;

            // get the total remaining entries
            remaining = query.getResultSize();
            // reset query
            query.startOffset(0);

            log.info("Deleted {} entries in this iteration. Total remaining: {}.", len, remaining);

        } while (remaining > 0);

        log.info("Removed tenant ({}) from system (total: {}).", tenantId, count);
    }

    private Query createQueryX(final CleanerConfig config, final RemoteCache<DeviceKey, DeviceInformation> devicesCache, final String tenantId) {

        // ISPN-11013: if we only select the "deviceId" field here, the we would
        // also need to index this field. So for the moment, we fetch the full object.

        final QueryFactory queryFactory = Search.getQueryFactory(devicesCache);

        return queryFactory.from(DeviceInformation.class)
                .having("tenantId").eq(tenantId)
                .maxResults(config.getDeletionChunkSize())
                .build();

    }

    private Query createQuery(final CleanerConfig config, final RemoteCache<DeviceKey, DeviceInformation> devicesCache, final String tenantId) {

        final QueryFactory queryFactory = Search.getQueryFactory(devicesCache);

        return queryFactory
                .create(String.format("from %s where tenantId=:tenantId", DeviceInformation.class.getName()), IndexedQueryMode.BROADCAST)
                .maxResults(config.getDeletionChunkSize())
                .setParameter("tenantId",tenantId);

    }

    private void deleteDevice(final RemoteCache<DeviceKey, DeviceInformation> devicesCache, final RemoteCache<DeviceConnectionKey, String> devicesConnectionCache,
            final String tenantIdToClean, final String deviceId) {

        // delete device connection entry first

        if (devicesConnectionCache != null) {
            final DeviceConnectionKey conKey = new DeviceConnectionKey(tenantIdToClean, deviceId);
            devicesConnectionCache.remove(conKey);
        }

        // delete device registration second

        final DeviceKey regKey = DeviceKey
                .deviceKey(TenantHandle.of(tenantIdToClean, tenantIdToClean, null), deviceId);

        devicesCache.remove(regKey);
    }

}
