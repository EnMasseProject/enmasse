/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import io.enmasse.iot.service.base.AbstractProjectBasedService;
import io.opentracing.noop.NoopSpan;
import static io.vertx.core.Future.failedFuture;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.HttpURLConnection;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.client.ServerErrorException;
import org.eclipse.hono.service.tenant.TenantService;
import org.eclipse.hono.util.CacheDirective;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.TenantResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import io.enmasse.iot.model.v1.IoTProject;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@Service
public class TenantServiceImpl extends AbstractProjectBasedService implements TenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantServiceImpl.class);

    private static final TenantResult<JsonObject> RESULT_NOT_FOUND = TenantResult.from(HTTP_NOT_FOUND);

    protected TenantServiceConfigProperties configuration;

    @Autowired
    public void setConfig(final TenantServiceConfigProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public Future<TenantResult<JsonObject>> get(final String tenantName, final Span span) {

        logger.debug("Get tenant - name: {}", tenantName);

        span.log(ImmutableMap.<String,Object>builder()
                .put("event", "get tenant")
                .put("tenant_id", tenantName)
                .build());

        return getProject(tenantName)

                .map(project -> project
                        .map(p -> convertToHono(tenantName, p))
                        .orElse(RESULT_NOT_FOUND));

    }

    @Override
    public Future<TenantResult<JsonObject>> get(final X500Principal subjectDn, final Span span) {
        return failedFuture(new ServerErrorException(HttpURLConnection.HTTP_NOT_IMPLEMENTED));
    }

    @Override
    public Future<TenantResult<JsonObject>> get(String tenantId) {
        return get(tenantId, NoopSpan.INSTANCE);
    }

    @Override
    public Future<TenantResult<JsonObject>> get(X500Principal subjectDn) {
        return get(subjectDn, NoopSpan.INSTANCE);
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
