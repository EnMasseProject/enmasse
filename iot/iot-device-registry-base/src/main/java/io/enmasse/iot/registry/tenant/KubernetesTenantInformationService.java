/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.tenant;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.HttpURLConnection;
import java.util.Optional;

import io.enmasse.iot.model.v1.IoTProjectStatus;
import org.eclipse.hono.client.ClientErrorException;
import org.eclipse.hono.client.ServerErrorException;
import org.eclipse.hono.deviceregistry.service.tenant.TenantInformationService;
import org.eclipse.hono.deviceregistry.service.tenant.TenantKey;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.tenant.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.service.base.AbstractProjectBasedService;
import io.opentracing.Span;
import io.vertx.core.Future;

public class KubernetesTenantInformationService extends AbstractProjectBasedService implements TenantInformationService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesTenantInformationService.class);

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Future<Result<TenantKey>> tenantExists(String tenantId, Span span) {

        return getProject(tenantId)
                .flatMap(project -> {
                            if (!project.isEmpty()) {
                                return validateTenant(project, tenantId, HTTP_NOT_FOUND)
                                        .compose(tenant -> Future.succeededFuture(
                                                OperationResult.ok(
                                                        HttpURLConnection.HTTP_OK,
                                                        TenantKey.from(tenant.getId(), tenant.getName()),
                                                        Optional.empty(),
                                                        Optional.empty())
                                                )
                                        );
                            } else {
                                return Future.succeededFuture(Result.from(HTTP_NOT_FOUND));
                            }
                        }
                );
    }

    /**
     * A variant of a call that returns {@link TenantInformation} object with more data. Used by authentication provider.
     *
     * @param tenantName The name of the tenant.
     * @param notFoundStatusCode The status code to be returned when tenant is not found.
     * @param span The tracing span.
     *
     * @return The future containing tenant information if found or failed future with {@code notFoundStatusCode} status otherwise.
     */
    public Future<TenantInformation> tenantExists(final String tenantName, final int notFoundStatusCode, final Span span) {

        return getProject(tenantName)
                .flatMap(project -> validateTenant(project, tenantName, notFoundStatusCode));

    }

    public Future<Optional<Tenant>> getTenant(TenantKey tenantKey) {
        return getProject(tenantKey.getName())
                .flatMap(project -> validateTenant(project, tenantKey.getName(), HTTP_NOT_FOUND)
                                        .compose(tenant -> Future.succeededFuture(tenant.getTenant())));
    }

    private Future<TenantInformation> validateTenant(final Optional<IoTProject> projectResult, final String tenantName, final int notFoundStatusCode) {

        if (projectResult.isEmpty()) {
            return notFound(notFoundStatusCode, "Tenant '" + tenantName + "' does not exist");
        }

        final IoTProject project = projectResult.orElseThrow();

        if (project.getMetadata().getDeletionTimestamp() != null) {
            // project is being deleted
            return notFound(notFoundStatusCode, "Tenant '" + tenantName + "' scheduled for deletion");
        }

        if (project.getMetadata().getCreationTimestamp() == null) {
            log.debug("Empty creation timestamp for tenant '" + tenantName + "'");
            return failedFuture(new ServerErrorException(HTTP_INTERNAL_ERROR, "Empty creation timestamp"));
        }

        final Tenant tenant;
        try {
            // we re-encode the accepted configuration into the Hono management structure,
            // which isn't completely correct as the Hono 'Tenant' object is not the same
            // as the Hono 'TenantObject'.
            //
            // it may be that the next step fails, in that case we simply report this as an error
            var json = this.mapper.valueToTree(project.getStatus().getAccepted().getConfiguration());
            tenant = this.mapper.treeToValue(json, Tenant.class);
        } catch (Exception e) {
            return failedFuture(e);
        }

        return succeededFuture(TenantInformation.of(project.getMetadata().getNamespace(), project.getMetadata().getName(), tenantName + "/" + project.getMetadata().getCreationTimestamp(), tenant));
    }

    /**
     * Create a completed future with a "not found" result.
     *
     * @param notFoundStatusCode The status code to use.
     * @param message The message to use.
     * @return The completed future, never returns {@code null}.
     */
    private static <T> Future<T> notFound(final int notFoundStatusCode, final String message) {
        log.debug(message);
        return failedFuture(new ClientErrorException(notFoundStatusCode, message));
    }
}
