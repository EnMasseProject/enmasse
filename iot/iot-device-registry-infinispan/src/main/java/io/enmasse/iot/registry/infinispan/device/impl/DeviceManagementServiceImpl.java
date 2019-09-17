/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.hono.service.management.OperationResult.ok;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.service.base.infinispan.cache.AdapterCredentialsCacheProvider;
import io.enmasse.iot.service.base.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.AbstractDeviceManagementService;
import io.enmasse.iot.service.base.infinispan.device.DeviceInformation;
import io.enmasse.iot.service.base.infinispan.device.DeviceKey;
import io.opentracing.Span;
import io.vertx.core.json.Json;

@Component
public class DeviceManagementServiceImpl extends AbstractDeviceManagementService {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementServiceImpl.class);

    @Autowired
    public DeviceManagementServiceImpl(final DeviceManagementCacheProvider managementProvider, final AdapterCredentialsCacheProvider adapterProvider) {
        super(managementProvider, adapterProvider);
    }

    @Override
    protected CompletableFuture<OperationResult<Id>> processCreateDevice(final DeviceKey key, final Device device, final Span span) {

        final DeviceInformation value = new DeviceInformation();
        value.setTenantId(key.getTenantId());
        value.setDeviceId(key.getDeviceId());
        value.setRegistrationInformation(Json.encode(device));

        return this.managementCache
                .putIfAbsentAsync(key, value)
                .thenApply(result -> {
                    if (result == null) {
                        return OperationResult.ok(HTTP_CREATED,
                                Id.of(key.getDeviceId()),
                                Optional.empty(),
                                Optional.of(value.getVersion()));
                    } else {
                        return OperationResult.empty(HTTP_CONFLICT);
                    }
                });

    }

    @Override
    protected CompletableFuture<OperationResult<Device>> processReadDevice(final DeviceKey key, final Span span) {

        return this.managementCache
                .getWithMetadataAsync(key)
                .thenApply(result -> {

                    if (result == null) {
                        log.debug("Device {} not found", key);
                        return OperationResult.empty(HTTP_NOT_FOUND);
                    }

                    return OperationResult.ok(HTTP_OK,
                            result.getValue().getRegistrationInformationAsJson().mapTo(Device.class),
                            Optional.empty(),
                            Optional.ofNullable(result.getValue().getVersion()));

                });

    }

    @Override
    protected CompletableFuture<OperationResult<Id>> processUpdateDevice(final DeviceKey key, final Device device, final Optional<String> resourceVersion, final Span span) {

        return this.managementCache

                .getWithMetadataAsync(key)
                .thenCompose(currentValue -> {

                    if (currentValue == null) {
                        return completedFuture(OperationResult.empty(HTTP_NOT_FOUND));
                    }

                    if (!currentValue.getValue().isVersionMatch(resourceVersion)) {
                        return failedFuture(new ServiceInvocationException(HTTP_PRECON_FAILED, "Version mismatch"));
                    }

                    final DeviceInformation newValue = currentValue.getValue().newVersion();
                    newValue.setRegistrationInformation(Json.encode(device));

                    return this.managementCache

                            .replaceWithVersionAsync(key, newValue, currentValue.getVersion())
                            .thenApply(putResult -> {
                                if (putResult == null) {
                                    return OperationResult.empty(HTTP_PRECON_FAILED);
                                }

                                return ok(
                                        HTTP_NO_CONTENT,
                                        Id.of(key.getDeviceId()),
                                        Optional.empty(),
                                        Optional.ofNullable(newValue.getVersion()));
                            });

                });
    }

    protected CompletableFuture<Result<Void>> processDeleteDevice(final DeviceKey key, final Optional<String> resourceVersion, final Span span) {

        return this.managementCache
                .getWithMetadataAsync(key)
                .thenCompose(result -> {

                    if (result == null) {
                        return completedFuture(Result.from(HTTP_NOT_FOUND));
                    }

                    if (!result.getValue().isVersionMatch(resourceVersion)) {
                        return completedFuture(Result.from(HTTP_PRECON_FAILED));
                    }

                    return this.managementCache
                            .removeWithVersionAsync(key, result.getVersion())
                            .thenApply(removeResult -> {
                                return Result.from(HTTP_NO_CONTENT);
                            });

                });
    }

}
