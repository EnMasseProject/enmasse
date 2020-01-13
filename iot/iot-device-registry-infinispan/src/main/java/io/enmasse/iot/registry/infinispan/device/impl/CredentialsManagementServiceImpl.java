/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static io.enmasse.iot.infinispan.device.DeviceKey.deviceKey;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.infinispan.client.hotrod.RemoteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.infinispan.device.DeviceCredential;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.registry.device.AbstractCredentialsManagementService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;

@Component
public class CredentialsManagementServiceImpl extends AbstractCredentialsManagementService {

    // Adapter cache :
    // <(tenantId + authId + type), (credential + deviceId)>
    private final RemoteCache<io.enmasse.iot.infinispan.device.CredentialKey, String> adapterCache;

    // Management cache
    // <(tenantId + deviceId), (device information + version + credentials)>
    private final RemoteCache<io.enmasse.iot.infinispan.device.DeviceKey, DeviceInformation> managementCache;

    @Autowired
    public CredentialsManagementServiceImpl(final DeviceManagementCacheProvider cacheProvider, final HonoPasswordEncoder passwordEncoder) {
        super(passwordEncoder);
        this.managementCache = cacheProvider
                .getDeviceManagementCache()
                .orElseThrow(() -> new NoSuchElementException("Missing device management cache"));
        this.adapterCache = cacheProvider
                .getAdapterCredentialsCache()
                .orElseThrow(() -> new NoSuchElementException("Missing adapter credentials cache"));
    }

    @Override
    protected CompletableFuture<OperationResult<Void>> processSet(final DeviceKey key, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span) {

        return this.managementCache

                .getWithMetadataAsync(deviceKey(key))
                .thenCompose(currentValue -> {

                    if (currentValue == null) {
                        return completedFuture(empty(HTTP_NOT_FOUND));
                    }

                    if (!currentValue.getValue().isVersionMatch(resourceVersion)) {
                        return failedFuture(new ServiceInvocationException(HTTP_PRECON_FAILED, "Version mismatch"));
                    }

                    final DeviceInformation newValue = currentValue.getValue().newVersion();
                    newValue.setCredentials(toInternal(credentials));

                    final Collection<io.enmasse.iot.infinispan.device.CredentialKey> affectedKeys;
                    try {
                        affectedKeys = calculateDifference(key.getTenantId(), currentValue.getValue().getCredentials(), newValue.getCredentials());
                    } catch (final IllegalStateException e) {
                        return completedFuture(OperationResult.empty(HTTP_BAD_REQUEST));
                    }

                    return this.managementCache
                            .replaceWithVersionAsync(deviceKey(key), newValue, currentValue.getVersion())

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

    static Collection<io.enmasse.iot.infinispan.device.CredentialKey> calculateDifference(final String tenantId, final List<DeviceCredential> current, final List<DeviceCredential> next) {

        final Set<io.enmasse.iot.infinispan.device.CredentialKey> result = new HashSet<>();

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

    private static Map<io.enmasse.iot.infinispan.device.CredentialKey, DeviceCredential> toMap(final String tenantId, final List<DeviceCredential> entries) {

        // if the map is null or empty ...

        if ( entries == null || entries.isEmpty()) {
            // ... directly return an empty result
            return new HashMap<>();
        }

        final Map<io.enmasse.iot.infinispan.device.CredentialKey, DeviceCredential> result = new HashMap<>(entries.size());

        for (final DeviceCredential credential : entries) {

            final var key = io.enmasse.iot.infinispan.device.CredentialKey.credentialKey(tenantId, credential.getAuthId(), credential.getType());
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
    protected CompletableFuture<?> clearAdapterEntries(final Collection<io.enmasse.iot.infinispan.device.CredentialKey> keys) {
        final List<CompletableFuture<?>> futures = new ArrayList<>(keys.size());

        for (final io.enmasse.iot.infinispan.device.CredentialKey key : keys) {
            futures.add(this.adapterCache.removeAsync(key));
        }

        return MoreFutures.allOf(futures);
    }

    @Override
    protected CompletableFuture<OperationResult<List<CommonCredential>>> processGet(final DeviceKey key, final Span span) {

        return this.managementCache

                .getAsync(deviceKey(key))
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
