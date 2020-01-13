/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.infinispan.config.InfinispanProperties;
import io.enmasse.iot.registry.health.AbstractSyncHealthCheck;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.Status;

@Component
@ConditionalOnExpression("${enmasse.iot.registry.infinispan.uploadSchema:" + InfinispanProperties.DEFAULT_UPLOAD_SCHEMA + "}")
public class ProtobufSchemaCheck extends AbstractSyncHealthCheck {

    private DeviceManagementCacheProvider provider;

    @Autowired
    public ProtobufSchemaCheck(final Vertx vertx, final DeviceManagementCacheProvider provider) {
        super(vertx, "protobuf/schema");
        this.provider = provider;
    }

    @Override
    protected Status checkLivenessSync() {
        try {
            this.provider.checkSchema();
            return Status.OK();
        } catch (Exception e) {
            return KO("Failed to check protobuf schema", e);
        }
    }

}
