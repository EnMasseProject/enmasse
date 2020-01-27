/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.device;

import static io.enmasse.iot.registry.device.CredentialKey.credentialKey;
import static io.enmasse.iot.utils.MoreFutures.finishHandler;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.util.CredentialsResult;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.registry.tenant.TenantInformation;
import io.enmasse.iot.registry.tenant.TenantInformationService;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public abstract class AbstractCredentialsService implements CredentialsService {

    @Autowired
    protected TenantInformationService tenantInformationService;

    public void setTenantInformationService(TenantInformationService tenantInformationService) {
        this.tenantInformationService = tenantInformationService;
    }

    @Override
    public void get(final String tenantId, final String type, final String authId, final Span span, final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        finishHandler(() -> processGet(tenantId, type, authId, span), resultHandler);
    }

    @Override
    public void get(String tenantId, String type, String authId, JsonObject clientContext, Span span, Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        get(tenantId, type, authId, span, resultHandler);
    }

    protected Future<CredentialsResult<JsonObject>> processGet(final String tenantId, final String type, final String authId, final Span span) {

        return this.tenantInformationService
                .tenantExists(tenantId, HTTP_NOT_FOUND, span.context())
                .flatMap(tenantHandle -> processGet(tenantHandle, credentialKey(tenantHandle, authId, type), span));

    }

    protected abstract Future<CredentialsResult<JsonObject>> processGet(TenantInformation tenant, CredentialKey key, Span span);

}
