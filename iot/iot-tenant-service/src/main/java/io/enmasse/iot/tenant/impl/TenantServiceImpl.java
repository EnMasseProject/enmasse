/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import static java.util.Objects.requireNonNull;

import java.net.HttpURLConnection;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.util.CacheDirective;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.TenantResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.IoTProjects;
import io.enmasse.iot.model.IoTProjects.Client;
import io.enmasse.iot.model.v1.IoTProject;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@Service
@Scope("prototype")
public class TenantServiceImpl extends AbstractKubernetesTenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantServiceImpl.class);

    private static final TenantResult<JsonObject> RESULT_NOT_FOUND = TenantResult
            .from(HttpURLConnection.HTTP_NOT_FOUND);

    @FunctionalInterface
    private interface TenantOperation {
        TenantResult<JsonObject> run(KubernetesClient client) throws Exception;
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    protected void withClient(
            final TenantOperation operation,
            final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {

        requireNonNull(operation);
        requireNonNull(resultHandler);

        final Optional<KubernetesClient> client = getClient();

        if (!client.isPresent()) {
            logger.warn("No client is present. Failing operation...");
            resultHandler.handle(Future.failedFuture("No Kubernetes client present"));
        }

        callBlocking(() -> {
            try {
                return operation.run(client.get());
            } catch (final Exception e) {
                logger.info("Failed to execute operation", e);
                throw e;
            }
        }, resultHandler);

    }

    @Override
    public void get(
            final String tenantId, final Span span,
            final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {

        logger.trace("Get - Tenant Id: {}", tenantId);

        withClient(client -> {

            final Client projects = IoTProjects.forClient(client);
            return getTenant(projects, tenantId);

        }, resultHandler);

    }

    @Override
    public void get(final X500Principal subjectDn, final Span span,
            final Handler<AsyncResult<TenantResult<JsonObject>>> resultHandler) {

        super.get(subjectDn, span, resultHandler);

    }

    private TenantResult<JsonObject> getTenant(final Client projects, final String tenantId) throws Exception {

        final String[] toks = tenantId.split("\\.", 2);

        if (toks.length < 2) {
            return RESULT_NOT_FOUND;
        }

        final String namespace = toks[0];
        final String name = toks[1];

        final IoTProject project = projects
                .inNamespace(namespace)
                .withName(name).get();

        if (project == null) {
            return RESULT_NOT_FOUND;
        }

        JsonObject payload;
        if (project.getSpec().getConfiguration() != null) {
            payload = new JsonObject(this.objectMapper.writeValueAsString(project.getSpec().getConfiguration()));
        } else {
            payload = new JsonObject();
        }

        payload.put(Constants.JSON_FIELD_TENANT_ID, tenantId);

        return TenantResult.from(
                HttpURLConnection.HTTP_OK,
                payload,
                CacheDirective.maxAgeDirective(this.configuration.getCacheTimeToLive().getSeconds()));
    }

}
