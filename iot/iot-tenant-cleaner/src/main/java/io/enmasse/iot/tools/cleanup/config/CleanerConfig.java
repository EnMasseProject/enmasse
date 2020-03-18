/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tools.cleanup.config;

import java.util.LinkedList;

import com.google.common.base.MoreObjects;

import io.enmasse.iot.infinispan.config.InfinispanProperties;

public class CleanerConfig {

    /**
     * Default value of "indexBroadcastQuery".
     * <br>
     * We must set the "BROADCAST" flag, as we rely on the "near realtime" indexer.
     */
    private static final boolean DEFAULT_INDEX_BROADCAST_QUERY = true;

    public static final int DEFAULT_DELETION_CHUNK_SIZE = 100;

    private String tenantId;

    private boolean indexBroadcastQuery = DEFAULT_INDEX_BROADCAST_QUERY;

    private InfinispanProperties infinispan = new InfinispanProperties();

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isIndexBroadcastQuery() {
        return this.indexBroadcastQuery;
    }

    public void setIndexBroadcastQuery(boolean indexBroadcastQuery) {
        this.indexBroadcastQuery = indexBroadcastQuery;
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
        if (this.infinispan.getCacheNames().getDevices().isBlank()) {
            result.add(missingField("infinispan.devicesCacheName"));
        }
        if (this.infinispan.getCacheNames().getDeviceConnections().isBlank()) {
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tenantId", this.tenantId)
                .add("infinispan", this.infinispan)
                .toString();
    }

}
