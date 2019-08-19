/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static io.enmasse.iot.registry.infinispan.device.data.DeviceKey.device;
import static io.enmasse.iot.registry.infinispan.util.Credentials.fromInternal;
import static io.enmasse.iot.registry.infinispan.util.Credentials.toInternal;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.hono.service.management.OperationResult.empty;
import static org.eclipse.hono.service.management.OperationResult.ok;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.registry.infinispan.cache.AdapterCredentialsCacheProvider;
import io.enmasse.iot.registry.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.device.AbstractCredentialsManagementService;
import io.enmasse.iot.registry.infinispan.device.data.CredentialsKey;
import io.enmasse.iot.registry.infinispan.device.data.DeviceCredential;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import io.enmasse.iot.registry.infinispan.util.MoreFutures;
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

        final DeviceKey key = device(tenantId, deviceId);

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

                    final Collection<CredentialsKey> affectedKeys;
                    try {
                        affectedKeys = calculateDifference(tenantId, currentValue.getValue().getCredentials(), newValue.getCredentials());
                    } catch (final IllegalStateException e) {
                        return completedFuture(OperationResult.empty(HTTP_BAD_REQUEST));
                    }

                    return this.managementCache
                            .replaceWithVersionAsync(key, newValue, currentValue.getVersion())

                            .<OperationResult<Void>>thenCompose(lockResult -> {

                                if (lockResult == null) {
                                    return failedFuture(new IllegalStateException("Replace returned 'null'"));
                                }

                                if (lockResult == false) {
                                    return completedFuture(OperationResult.empty(HTTP_PRECON_FAILED));
                                }

                                return completedFuture(OperationResult.empty(HTTP_NO_CONTENT));

                            })

                            // after replacing the entry ... trigger deletion of the adapter entries

                            .thenCompose(replaceResult -> {
                                return clearAdapterEntries(affectedKeys)
                                            .thenApply(x -> replaceResult);
                            });

                });

    }

    static Collection<CredentialsKey> calculateDifference(final String tenantId, final List<DeviceCredential> current, final List<DeviceCredential> next) {

        final Set<CredentialsKey> result = new HashSet<>();

        final var currentMap = toMap(tenantId, current);
        final var nextMap = toMap(tenantId, next);

        for (final var entry : nextMap.entrySet()) {
            final var currentEntry = currentMap.remove(entry.getKey());

            if ( currentEntry == null ) {
                // entry was added ... clear in any case
                result.add(entry.getKey());
                continue;
            }

            if ( !currentEntry.equals(entry.getValue() )) {
                // entry changed
                result.add(entry.getKey());
                continue;
            }

            // entry is equal ... continue ...
        }

        // add deletions

        result.addAll(currentMap.keySet());

        // return result

        return result;

    }

    private static Map<CredentialsKey, DeviceCredential> toMap(String tenantId, final List<DeviceCredential> entries) {
        final Map<CredentialsKey, DeviceCredential> result = new HashMap<>(entries.size());

        for (final DeviceCredential credential : entries) {

            final var key = new CredentialsKey(tenantId, credential.getAuthId(), credential.getType());
            if (result.put(key, credential) != null) {
                // conflict adding
                throw new IllegalStateException(String.format("Duplicate entries for '%s'", key));
            }

        }

        return result;
    }

    /**
     * Delete keys in the adapter cache.
     *
     * @param keys The keys to delete. May be empty, but must not be {@code null}.
     * @return The future which completes when all the delete operations are completed.
     */
    protected CompletableFuture<?> clearAdapterEntries(final Collection<CredentialsKey> keys) {
        final List<CompletableFuture<?>> futures = new ArrayList<>(keys.size());

        for (final CredentialsKey key : keys) {
            futures.add(this.adapterCache.removeAsync(key));
        }

        return MoreFutures.allOf(futures);
    }

    @Override
    protected CompletableFuture<OperationResult<List<CommonCredential>>> processGet(final String tenantId, final String deviceId, final Span span) {

        final DeviceKey key = device(tenantId, deviceId);

        return this.managementCache

                .getAsync(key)
                .thenApply(result -> {

                    if (result == null) {
                        return OperationResult.empty(HTTP_NOT_FOUND);
                    }

                    return ok(
                            HTTP_OK,
                            fromInternal(result.getCredentials()),
                            Optional.empty(),
                            Optional.ofNullable(result.getVersion()));
                });

    }

}
