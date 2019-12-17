/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.tenant;

import io.enmasse.iot.infinispan.tenant.TenantInformation;

import static io.enmasse.iot.infinispan.tenant.TenantInformation.of;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.client.ClientErrorException;
import org.eclipse.hono.client.ServerErrorException;
import org.eclipse.hono.service.management.tenant.Tenant;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.service.base.AbstractProjectBasedService;
import io.opentracing.Span;

@Component
public class KubernetesTenantInformationService extends AbstractProjectBasedService implements TenantInformationService {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public CompletableFuture<TenantInformation> tenantExists(final String tenantName, final int notFoundStatusCode, final Span span) {

        return getProject(tenantName)
                .thenCompose(project -> validateTenant(project, tenantName, notFoundStatusCode));

    }

    private CompletableFuture<TenantInformation> validateTenant(final Optional<IoTProject> projectResult, final String tenantName, final int notFoundStatusCode) {

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

        return completedFuture(of(tenantName, tenantName + "/" + project.getMetadata().getCreationTimestamp(), tenant));
    }

    /**
     * Create a completed future with a "not found" result.
     *
     * @param notFoundStatusCode The status code to use.
     * @param message The message to use.
     * @return The completed future, never returns {@code null}.
     */
    private static <T> CompletableFuture<T> notFound(final int notFoundStatusCode, final String message) {
        return failedFuture(new ClientErrorException(notFoundStatusCode, message));
    }

}
