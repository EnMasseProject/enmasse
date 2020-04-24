/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_ADAPTER;
import io.vertx.core.json.JsonArray;
import static io.vertx.core.json.JsonObject.mapFrom;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.eclipse.hono.util.RegistrationResult.from;

import org.eclipse.hono.deviceregistry.service.device.AbstractRegistrationService;
import org.eclipse.hono.deviceregistry.service.device.DeviceKey;
import org.eclipse.hono.util.RegistrationConstants;
import org.eclipse.hono.util.RegistrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.enmasse.iot.jdbc.store.device.AbstractDeviceAdapterStore;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@Component
@Profile(PROFILE_REGISTRY_ADAPTER)
public class RegistrationServiceImpl extends AbstractRegistrationService {

    private final AbstractDeviceAdapterStore store;

    @Autowired
    public RegistrationServiceImpl(final AbstractDeviceAdapterStore store) {
        this.store = store;
    }

    @Override
    protected Future<RegistrationResult> processAssertRegistration(DeviceKey deviceKey, Span span) {
        return this.store.readDevice(deviceKey, span.context())
                .map(r -> {

                    if (r.isPresent()) {

                        var result = r.get();
                        var data = mapFrom(result.getDevice());
                        var payload = new JsonObject()
                                .put(RegistrationConstants.FIELD_DATA, data);
                        return from(HTTP_OK, payload, null);

                    } else {

                        return from(HTTP_NOT_FOUND);

                    }

                });
    }

    @Override
    protected Future<JsonArray> resolveGroupMembers(String tenantId, JsonArray viaGroups, Span span) {
        //TODO implement with https://github.com/EnMasseProject/enmasse/issues/4339
        return null;
    }
}
