/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.client.ServerErrorException;
import org.eclipse.hono.util.CacheDirective;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.TenantResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.enmasse.iot.model.v1.IoTProject;
import io.opentracing.Span;
import io.vertx.core.json.JsonObject;

@Service
public class TenantServiceImpl extends AbstractKubernetesTenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantServiceImpl.class);

    private static final TenantResult<JsonObject> RESULT_NOT_FOUND = TenantResult.from(HTTP_NOT_FOUND);

    @Override
    protected CompletableFuture<TenantResult<JsonObject>> processGet(final String tenantName, final Span span) {

        logger.debug("Get tenant - name: {}", tenantName);

        return getProject(tenantName)

                .thenApply(project -> project
                        .map(p -> convertToHono(tenantName, p))
                        .orElse(RESULT_NOT_FOUND));
    }

    @Override
    protected CompletableFuture<TenantResult<JsonObject>> processGet(final X500Principal subjectDn, final Span span) {
        return failedFuture(new ServerErrorException(HttpURLConnection.HTTP_NOT_IMPLEMENTED));
    }

    private TenantResult<JsonObject> convertToHono(final String tenantName, final IoTProject project) {

        final JsonObject payload;
        if (project.getSpec().getConfiguration() != null) {
            payload = JsonObject.mapFrom(project.getSpec().getConfiguration());
        } else {
            payload = new JsonObject();
        }

        payload.put(Constants.JSON_FIELD_TENANT_ID, tenantName);

        return TenantResult.from(
                HttpURLConnection.HTTP_OK,
                payload,
                CacheDirective.maxAgeDirective(this.configuration.getCacheTimeToLive().getSeconds()));
    }

}
