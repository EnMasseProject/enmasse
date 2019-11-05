/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.tenant;

import io.enmasse.iot.infinispan.tenant.TenantHandle;
import static io.enmasse.iot.infinispan.tenant.TenantHandle.of;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.client.ClientErrorException;
import org.eclipse.hono.client.ServerErrorException;
import org.springframework.stereotype.Component;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.service.base.AbstractProjectBasedService;
import io.opentracing.Span;

@Component
public class KubernetesTenantInformationService extends AbstractProjectBasedService implements TenantInformationService {

    @Override
    public CompletableFuture<TenantHandle> tenantExists(final String tenantName, final int notFoundStatusCode, final Span span) {

        return getProject(tenantName)
                .thenCompose(project -> validateTenant(project, tenantName, notFoundStatusCode));

    }

    private CompletableFuture<TenantHandle> validateTenant(final Optional<IoTProject> projectResult, final String tenantName, final int notFoundStatusCode) {

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

        return completedFuture(of(tenantName, tenantName + "/" + project.getMetadata().getCreationTimestamp()));
    }

    /**
     * Create a completed future with a "not found" result.
     *
     * @param notFoundStatusCode The status code to use.
     * @param message The message to use.
     * @return The completed future, never returns {@code null}.
     */
    private static CompletableFuture<TenantHandle> notFound(final int notFoundStatusCode, final String message) {
        return failedFuture(new ClientErrorException(notFoundStatusCode, message));
    }

}
