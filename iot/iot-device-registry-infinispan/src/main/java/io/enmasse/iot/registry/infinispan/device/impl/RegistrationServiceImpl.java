/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static io.enmasse.iot.infinispan.device.DeviceKey.deviceKey;
import static io.enmasse.iot.registry.infinispan.Profiles.PROFILE_DEVICE_REGISTRY;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import org.eclipse.hono.util.RegistrationConstants;
import org.eclipse.hono.util.RegistrationResult;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.registry.device.AbstractRegistrationService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@Component
@Profile(PROFILE_DEVICE_REGISTRY)
public class RegistrationServiceImpl extends AbstractRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    // Management cache
    // <(TenantId+DeviceId), (Device information + version + credentials)>
    private final RemoteCache<io.enmasse.iot.infinispan.device.DeviceKey, DeviceInformation> managementCache;

    public RegistrationServiceImpl(final DeviceManagementCacheProvider provider) {
        this.managementCache = provider.getOrCreateDeviceManagementCache();
    }

    @Override
    protected Future<RegistrationResult> processGetDevice(final DeviceKey key, final Span span) {

        var f = this.managementCache

                .getWithMetadataAsync(deviceKey(key))
                .thenApply(result -> {

                    if (result == null) {
                        return RegistrationResult.from(HTTP_NOT_FOUND);
                    }

                    log.debug("Found device: {}", result);

                    return RegistrationResult.from(HTTP_OK, convertTo(result.getValue().getRegistrationInformationAsJson()));
                });

        return MoreFutures.map(f);

    }

    private static JsonObject convertTo(final JsonObject deviceInfo) {
        return new JsonObject()
                .put(RegistrationConstants.FIELD_DATA, deviceInfo);
    }

}
