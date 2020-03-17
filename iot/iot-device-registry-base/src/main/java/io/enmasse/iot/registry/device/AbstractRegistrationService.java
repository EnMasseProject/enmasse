/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.device;

import static io.enmasse.iot.registry.device.DeviceKey.deviceKey;
import static io.enmasse.iot.utils.MoreFutures.finishHandler;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import org.eclipse.hono.util.RegistrationResult;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.registry.tenant.TenantInformationService;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public abstract class AbstractRegistrationService extends org.eclipse.hono.service.registration.AbstractRegistrationService {

    @Autowired
    protected TenantInformationService tenantInformationService;

    public void setTenantInformationService(final TenantInformationService tenantInformationService) {
        this.tenantInformationService = tenantInformationService;
    }

    @Override
    protected void getDevice(final String tenantId, final String deviceId, final Span span, final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        finishHandler(() -> processGetDevice(tenantId, deviceId, span), resultHandler);
    }

    protected Future<RegistrationResult> processGetDevice(final String tenantName, final String deviceId, final Span span) {

        return this.tenantInformationService
                .tenantExists(tenantName, HTTP_NOT_FOUND, span.context())
                .flatMap(tenantId -> processGetDevice(deviceKey(tenantId, deviceId), span));

    }

    protected abstract Future<RegistrationResult> processGetDevice(DeviceKey key, Span span);

}
