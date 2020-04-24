/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_MANAGEMENT;
import static io.enmasse.iot.utils.MoreThrowables.hasCauseOf;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.eclipse.hono.service.management.OperationResult.empty;
import static org.eclipse.hono.service.management.OperationResult.ok;
import static org.eclipse.hono.util.CacheDirective.maxAgeDirective;

import java.util.Optional;
import java.util.UUID;

import org.eclipse.hono.deviceregistry.service.device.AbstractDeviceManagementService;
import org.eclipse.hono.deviceregistry.service.device.DeviceKey;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.util.CacheDirective;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.enmasse.iot.jdbc.store.DuplicateKeyException;
import io.enmasse.iot.jdbc.store.device.AbstractDeviceManagementStore;
import io.enmasse.iot.registry.jdbc.config.DeviceServiceProperties;
import io.opentracing.Span;
import io.vertx.core.Future;

@Component
@Profile(PROFILE_REGISTRY_MANAGEMENT)
public class DeviceManagementServiceImpl extends AbstractDeviceManagementService {

    private final AbstractDeviceManagementStore store;
    private final Optional<CacheDirective> ttl;

    @Autowired
    public DeviceManagementServiceImpl(final AbstractDeviceManagementStore store, final DeviceServiceProperties properties) {
        this.store = store;
        this.ttl = of(maxAgeDirective(properties.getRegistrationTtl().toSeconds()));
    }

    @Override
    protected Future<OperationResult<Id>> processCreateDevice(final DeviceKey key, final Device device, final Span span) {

        return this.store.createDevice(key, device, span.context())
                .map(r -> {
                    return ok(HTTP_CREATED, Id.of(key.getDeviceId()), empty(), empty());
                })
                .recover(e -> {
                    if (hasCauseOf(e, DuplicateKeyException.class)) {
                        return Future.succeededFuture(empty(HTTP_CONFLICT));
                    }
                    return Future.failedFuture(e);
                });

    }

    @Override
    protected Future<OperationResult<Device>> processReadDevice(final DeviceKey key, final Span span) {

        return this.store.readDevice(key, span.context())
                .<OperationResult<Device>>map(r -> {

                    if (r.isPresent()) {
                        var result = r.get();
                        return OperationResult.ok(
                                HTTP_OK,
                                result.getDevice(),
                                this.ttl,
                                result.getResourceVersion());
                    } else {
                        return empty(HTTP_NOT_FOUND);
                    }

                });

    }

    @Override
    protected Future<OperationResult<Id>> processUpdateDevice(final DeviceKey key, final Device device, final Optional<String> resourceVersion, final Span span) {

        return this.store.updateDevice(key, device, resourceVersion, span.context())

                .<OperationResult<Id>>map(r -> {
                    if (r.getUpdated() <= 0) {
                        return empty(HTTP_NOT_FOUND);
                    } else {
                        return empty(HTTP_NO_CONTENT);
                    }
                });

    }

    @Override
    protected Future<Result<Void>> processDeleteDevice(final DeviceKey key, final Optional<String> resourceVersion, final Span span) {

        return this.store.deleteDevice(key, resourceVersion, span.context())
                .map(r -> {
                    if (r.getUpdated() <= 0) {
                        return empty(HTTP_NOT_FOUND);
                    } else {
                        return empty(HTTP_NO_CONTENT);
                    }
                });

    }

    @Override
    protected String generateDeviceId(String tenantId) {
        return UUID.randomUUID().toString();
    }
}
