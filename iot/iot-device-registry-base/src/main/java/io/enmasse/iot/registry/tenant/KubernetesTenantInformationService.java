/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.tenant;

import static io.enmasse.iot.registry.tenant.TenantInformation.of;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

import java.util.Optional;

import org.eclipse.hono.client.ClientErrorException;
import org.eclipse.hono.client.ServerErrorException;
import org.eclipse.hono.service.management.tenant.Tenant;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.service.base.AbstractProjectBasedService;
import io.opentracing.SpanContext;
import io.vertx.core.Future;

@Component
public class KubernetesTenantInformationService extends AbstractProjectBasedService implements TenantInformationService {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Future<TenantInformation> tenantExists(final String tenantName, final int notFoundStatusCode, final SpanContext span) {

        return getProject(tenantName)
                .flatMap(project -> validateTenant(project, tenantName, notFoundStatusCode));

    }

    private Future<TenantInformation> validateTenant(final Optional<IoTProject> projectResult, final String tenantName, final int notFoundStatusCode) {

        if (projectResult.isEmpty()) {
            return notFound(notFoundStatusCode, "Tenant does not exist");
        }

        final IoTProject project = projectResult.orElseThrow();

        if (project.getMetadata().getDeletionTimestamp() != null) {
            // project is being deleted
            return notFound(notFoundStatusCode, "Tenant scheduled for deletion");
        }

        if (project.getMetadata().getCreationTimestamp() == null) {
            return failedFuture(new ServerErrorException(HTTP_INTERNAL_ERROR, "Empty creation timestamp"));
        }

        final Tenant tenant;
        try {
            tenant = this.mapper.treeToValue(project.getSpec().getConfiguration(), Tenant.class);
        } catch (JsonProcessingException e) {
            return failedFuture(e);
        }

        return succeededFuture(of(project.getMetadata().getNamespace(), project.getMetadata().getName(), tenantName + "/" + project.getMetadata().getCreationTimestamp(), tenant));
    }

    /**
     * Create a completed future with a "not found" result.
     *
     * @param notFoundStatusCode The status code to use.
     * @param message The message to use.
     * @return The completed future, never returns {@code null}.
     */
    private static <T> Future<T> notFound(final int notFoundStatusCode, final String message) {
        return failedFuture(new ClientErrorException(notFoundStatusCode, message));
    }

}
