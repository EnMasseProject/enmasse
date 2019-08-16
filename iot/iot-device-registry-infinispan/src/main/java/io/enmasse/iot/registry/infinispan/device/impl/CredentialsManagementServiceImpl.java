/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static io.enmasse.iot.registry.infinispan.util.Credentials.fromInternal;
import static io.enmasse.iot.registry.infinispan.util.Credentials.toInternal;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.hono.service.management.OperationResult.empty;
import static org.eclipse.hono.service.management.OperationResult.ok;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.infinispan.cache.AdapterCredentialsCacheProvider;
import io.enmasse.iot.registry.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.AbstractCredentialsManagementService;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.opentracing.Span;

@Component
public class CredentialsManagementServiceImpl extends AbstractCredentialsManagementService {

    @Autowired
    public CredentialsManagementServiceImpl(final DeviceManagementCacheProvider managementProvider, final AdapterCredentialsCacheProvider adapterProvider) {
        super(managementProvider, adapterProvider);
    }

    @Override
    protected CompletableFuture<OperationResult<Void>> processSet(final String tenantId, final String deviceId, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span) {

        final DeviceKey key = new DeviceKey(tenantId, deviceId);

        return this.managementCache

                .getWithMetadataAsync(key)
                .thenCompose(currentValue -> {

                    if (currentValue == null) {
                        return completedFuture(empty(HTTP_NOT_FOUND));
                    }

                    if (!currentValue.getValue().isVersionMatch(resourceVersion)) {
                        return failedFuture(new ServiceInvocationException(HTTP_PRECON_FAILED, "Version mismatch"));
                    }

                    final DeviceInformation newValue = currentValue.getValue().newVersion();
                    newValue.setCredentials(toInternal(credentials));

                    return this.managementCache
                            .replaceWithVersionAsync(key, newValue, currentValue.getVersion())
                            .thenCompose(lockResult -> {

                                if (lockResult == null) {
                                    return failedFuture(new IllegalStateException("Replace returned 'null'"));
                                }

                                if (lockResult == false) {
                                    return completedFuture(OperationResult.empty(HTTP_PRECON_FAILED));
                                }

                                return completedFuture(OperationResult.empty(HTTP_NO_CONTENT));

                            });

                });

    }

    @Override
    protected CompletableFuture<OperationResult<List<CommonCredential>>> processGet(final String tenantId, final String deviceId, final Span span) {

        final DeviceKey key = new DeviceKey(tenantId, deviceId);

        return this.managementCache

                .getWithMetadataAsync(key)
                .thenApply(result -> {

                    if (result == null) {
                        return OperationResult.empty(HTTP_NOT_FOUND);
                    }

                    return ok(
                            HTTP_OK,
                            fromInternal(result.getValue().getCredentials()),
                            Optional.empty(),
                            Optional.ofNullable(result.getValue().getVersion()));
                });

    }

}
