/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;

import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.hono.deviceregistry.FileBasedRegistrationService;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.registration.AbstractRegistrationService;
import org.eclipse.hono.service.registration.RegistrationService;
import org.eclipse.hono.tracing.TracingHelper;
import org.eclipse.hono.util.RegistrationConstants;
import org.eclipse.hono.util.RegistrationResult;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A Registration service that use an Infinispan as a backend service.
 * Infinispan is an open source project providing a distributed in-memory key/value data store
 *
 * <p>
 *@see <a href="https://infinspan.org">https://infinspan.org</a>
 *
 */
@Repository
@Primary
public class CacheRegistrationService extends AbstractVerticle implements RegistrationService, DeviceManagementService {
    private final RemoteCache<RegistrationKey, RegistryDeviceObject> registrationCache;

    @Autowired
    protected CacheRegistrationService(final RemoteCache<RegistrationKey, RegistryDeviceObject> cache) {
        this.registrationCache = cache;
    }

    /**
     * Registration service, based on {@link AbstractRegistrationService}.
     * <p>
     * This helps work around Java's inability to inherit from multiple base classes. We create a new Registration
     * service, overriding the implementation of {@link AbstractRegistrationService} with the implementation of our
     * {@link FileBasedRegistrationService#getDevice(String, String, Handler)}.
     */
    private final AbstractRegistrationService registrationService = new AbstractRegistrationService() {

        @Override
        public void getDevice(final String tenantId, final String deviceId,
                final Handler<AsyncResult<RegistrationResult>> resultHandler) {
            CacheRegistrationService.this.getDevice(tenantId, deviceId, resultHandler);
        }
    };

    @Override
    public void createDevice(final String tenantId, final Optional<String> deviceId, final Device device, final Span span,
            final Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final String deviceIdValue = deviceId.orElse(generateDeviceId());
        final RegistrationKey key = new RegistrationKey(tenantId, deviceIdValue);
        final RegistryDeviceObject deviceObject = new RegistryDeviceObject(device);

        registrationCache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsentAsync(key, deviceObject).thenAccept(result -> {
            if ( result == null){
                resultHandler.handle(Future.succeededFuture(
                        OperationResult.ok(HTTP_CREATED,
                                Id.of(deviceIdValue),
                                Optional.empty(),
                                Optional.ofNullable(deviceObject.getVersion()))));
            } else {
                TracingHelper.logError(span, "Device already exist for tenant");
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
            }
        });
    }

    @Override
    public void updateDevice(final String tenantId, final String deviceId, final Device device,
            final Optional<String> resourceVersion, final Span span, final Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);
        final RegistryDeviceObject deviceObject = new RegistryDeviceObject(device);
        AtomicBoolean doUpdate = new AtomicBoolean(true);

        resourceVersion.ifPresent(rv -> {
            registrationCache.getAsync(key).thenAccept(getResult -> {
                if (getResult == null) {
                    doUpdate.set(false);
                    TracingHelper.logError(span, "Device not found.");
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
                } else if (!getResult.getVersion().equals(rv)) {
                    doUpdate.set(false);
                    TracingHelper.logError(span, "Resource Version mismatch.");
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                }
            });
        });

        if (doUpdate.get()) {
            registrationCache.withFlags(Flag.FORCE_RETURN_VALUE).replaceAsync(key, deviceObject)
                    .thenAccept(updateResult -> {
                        if (updateResult == null) {
                            doUpdate.set(false);
                            TracingHelper.logError(span, "Device not found.");
                            resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
                        } else {
                            resultHandler.handle(Future.succeededFuture(
                                    OperationResult.ok(HTTP_NO_CONTENT, Id.of(deviceId), Optional.empty(),
                                            Optional.ofNullable(deviceObject.getVersion()))));
                        }
            });
        }
    }

    @Override
    public void deleteDevice(final String tenantId, final String deviceId, final Optional<String> resourceVersion,
            final Span span, final Handler<AsyncResult<Result<Void>>> resultHandler) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);
        AtomicBoolean doDelete = new AtomicBoolean(true);

        resourceVersion.ifPresent(rv -> {
            registrationCache.getAsync(key).thenAccept(getResult -> {
                if (getResult == null) {
                    doDelete.set(false);
                    TracingHelper.logError(span, "Device not found.");
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
                } else if (!getResult.getVersion().equals(rv)) {
                    doDelete.set(false);
                    TracingHelper.logError(span, "Resource Version mismatch.");
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                }
            });
        });

        if (doDelete.get()) {
            registrationCache.withFlags(Flag.FORCE_RETURN_VALUE).removeAsync(key).thenAccept(result -> {
                if (result == null) {
                    TracingHelper.logError(span, "Device not found.");
                    resultHandler.handle(Future.succeededFuture(Result.from(HTTP_NOT_FOUND)));
                } else {
                    resultHandler.handle(Future.succeededFuture(Result.from(HTTP_NO_CONTENT)));
                }
            });
        }
    }

    @Override
    public void readDevice(final String tenantId, final String deviceId,
            final Span span, final Handler<AsyncResult<OperationResult<Device>>> resultHandler) {

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(resultHandler);

        resultHandler.handle(Future.succeededFuture(readDevice(tenantId, deviceId, span).result()));
    }

    private Future<OperationResult<Device>> readDevice(final String tenantId, final String deviceId, final Span span) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);
        final Future<OperationResult<Device>> future = Future.future();

        registrationCache.withFlags(Flag.FORCE_RETURN_VALUE).getAsync(key).thenAccept( result -> {
            if (result == null){
                TracingHelper.logError(span, "Device not found.");
                future.complete(OperationResult.empty(HTTP_NOT_FOUND));
            } else {

                future.complete(OperationResult.ok(HTTP_OK,
                        result.getDevice(),
                        Optional.empty(),
                        Optional.ofNullable(result.getVersion())
                ));
            }
        });
        return future;
    }

    ///// AbstractRegistrationService redirects

    @Override
    public void assertRegistration(final String tenantId, final String deviceId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        registrationService.assertRegistration(tenantId, deviceId, resultHandler);
    }

    @Override
    public void assertRegistration(final String tenantId, final String deviceId, final String gatewayId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        registrationService.assertRegistration(tenantId, deviceId, gatewayId, resultHandler);
    }

    private void getDevice(final String tenantId, final String deviceId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(resultHandler);

        resultHandler.handle(Future.succeededFuture(convertResult(deviceId, readDevice(tenantId, deviceId, NoopSpan.INSTANCE).result())));
    }

    private RegistrationResult convertResult(final String deviceId, final OperationResult<Device> result) {
        return RegistrationResult.from(
                result.getStatus(),
                convertDevice(deviceId, result.getPayload()),
                result.getCacheDirective().orElse(null));
    }

    private static String generateDeviceId(){
        return UUID.randomUUID().toString();
    }

    private JsonObject convertDevice(final String deviceId, final Device payload) {

        if (payload == null) {
            return null;
        }

        final JsonObject data = JsonObject.mapFrom(payload);
        return new JsonObject()
                .put(RegistrationConstants.FIELD_PAYLOAD_DEVICE_ID, deviceId)
                .put("data", data);
    }

    private boolean versionCheck(final RegistryDeviceObject device, final Optional<String> version) {

        return device.getVersion().equals(version.orElse(device.getVersion()));
    }
}
