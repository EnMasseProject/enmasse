/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.device.impl;

import static io.enmasse.iot.infinispan.device.CredentialKey.credentialKey;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.time.Duration.between;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.hono.util.CacheDirective.maxAgeDirective;
import static org.eclipse.hono.util.CacheDirective.noCacheDirective;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

import org.eclipse.hono.service.management.tenant.Tenant;
import org.eclipse.hono.util.CacheDirective;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsResult;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.IndexedQueryMode;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.enmasse.iot.infinispan.cache.DeviceManagementCacheProvider;
import io.enmasse.iot.registry.infinispan.config.DeviceServiceProperties;
import io.enmasse.iot.registry.infinispan.device.AbstractCredentialsService;
import io.enmasse.iot.infinispan.device.CredentialKey;
import io.enmasse.iot.infinispan.device.DeviceCredential;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.infinispan.tenant.TenantHandle;
import io.enmasse.iot.registry.infinispan.util.Credentials;
import io.opentracing.Span;
import io.vertx.core.json.JsonObject;

@Component
public class CredentialsServiceImpl extends AbstractCredentialsService {

    private static final Logger log = LoggerFactory.getLogger(CredentialsServiceImpl.class);

    private final DeviceServiceProperties properties;

    private final ThreadPoolExecutor executor;

    private Duration defaultTtl;

    public CredentialsServiceImpl(final DeviceManagementCacheProvider cacheProvider, final DeviceServiceProperties properties) {
        super(cacheProvider);
        this.properties = properties;
        this.defaultTtl = this.properties.getCredentialsTtl();

        this.executor = new ThreadPoolExecutor(
                1, // core size
                Runtime.getRuntime().availableProcessors(), // maximum size
                1, TimeUnit.MINUTES, // idle timeout
                new LinkedBlockingQueue<>(this.properties.getTaskExecutorQueueSize()));
    }

    @PreDestroy
    public void dispose() {
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // failed to wait for shutdown
        }
    }

    @Override
    protected CompletableFuture<CredentialsResult<JsonObject>> processGet(final TenantHandle tenant, final CredentialKey key, final Span span) {

        return this.adapterCache
                .getWithMetadataAsync(key)
                .thenCompose(result -> {

                    // entry not found ...

                    if (result == null) {

                        // ... try to re-create cache entry

                        log.debug("Entry not found - resync: {}", key);
                        return resyncCacheEntry(tenant, key, span);
                    }

                    // entry found and in sync ... return

                    log.debug("Entry found: {}", result);

                    // get remaining ttl

                    final Duration ttl = calculateRemainingTtl(tenant, result);

                    log.debug("Remaining TTL: {}", ttl);

                    // return result

                    if (!result.getValue().isEmpty()) {
                        return completedFuture(found(new JsonObject(result.getValue()), ttl));
                    } else {
                        return completedFuture(notFound(ttl));
                    }

                });

    }

    private Duration calculateRemainingTtl(final TenantHandle tenant, final MetadataValue<?> result) {

        if (result.getLifespan() > 0 && result.getCreated() > 0) {

            final Instant eol =
                    ofEpochMilli(result.getCreated())
                            .plus(ofSeconds(result.getLifespan()));
            return between(now(), eol);

        } else {
            return getStoreTtl(tenant);
        }
    }

    private CompletionStage<CredentialsResult<JsonObject>> resyncCacheEntry(final TenantHandle tenant, final CredentialKey key, final Span span) {

        return searchCredentials(key)
                .thenCompose(r -> {

                    final var size = r.size();

                    log.debug("Found {} entries for {}", size, key);

                    switch (r.size()) {
                        case 0:
                            return storeNotFound(tenant, key);
                        case 1:
                            return storeCacheEntry(tenant, key, r.getFirst());
                        default:
                            log.warn("Found entry with multiple device mappings: {} -> {}", key, r);
                            return storeInvalidEntry(tenant);
                    }

                });

    }

    /**
     * Get the TTL for storing an entry in the adapter cache.
     *
     * @return The TTL. Must never return {@code null}.
     */
    private Duration getStoreTtl(final TenantHandle tenant) {

        return Optional
                .ofNullable(tenant.getTenant())
                .map(Tenant::getDefaults)
                .map(d -> d.get("ttl"))
                .flatMap(v -> {
                    if (v instanceof Number) {
                        return Optional.of(((Number) v).longValue());
                    } else if (v instanceof String) {
                        return Optional.of(Long.parseLong((String) v));
                    } else {
                        return Optional.empty();
                    }
                })
                .map(Duration::ofMillis)
                .orElse(this.defaultTtl);

    }

    private <T> CompletionStage<CredentialsResult<T>> storeInvalidEntry(final TenantHandle tenant) {
        return completedFuture(notFound(getStoreTtl(tenant)));
    }

    private CompletionStage<CredentialsResult<JsonObject>> storeCacheEntry(final TenantHandle tenant, final CredentialKey key, final JsonObject cacheEntry) {

        final Duration ttl = getStoreTtl(tenant);

        return this.adapterCache

                .putAsync(key, cacheEntry.encode(), ttl.toSeconds(), TimeUnit.SECONDS)
                .thenApply(putResult -> {

                    // we do not care about the result
                    return found(cacheEntry, ttl);

                });

    }

    private <T> CompletionStage<CredentialsResult<T>> storeNotFound(final TenantHandle tenant, final CredentialKey key) {

        final Duration ttl = getStoreTtl(tenant);

        return this.adapterCache

                .putIfAbsentAsync(key, "" /* empty entry */, ttl.toSeconds(), TimeUnit.SECONDS)
                .thenApply(putResult -> {

                    // we do not care about the result
                    return notFound(ttl);
                });

    }

    /**
     * Search for all credentials sets, which match tenant, authId, type.
     *
     * @param key The search key.
     * @return The result of the search.
     */
    private CompletableFuture<LinkedList<JsonObject>> searchCredentials(final CredentialKey key) {

        final QueryFactory queryFactory = Search.getQueryFactory(this.managementCache);

        final Query query = queryFactory
                .create(String.format("from %s d where d.tenantId=:tenantId and d.credentials.authId=:authId and d.credentials.type=:type", DeviceInformation.class.getName()), IndexedQueryMode.BROADCAST)
                .setParameter("tenantId", key.getTenantId())
                .setParameter("authId", key.getAuthId())
                .setParameter("type", key.getType());

        return CompletableFuture
                .supplyAsync(query::<DeviceInformation>list, this.executor)
                .thenApply(result -> mapCredentials(key, result));

    }

    private LinkedList<JsonObject> mapCredentials(final CredentialKey searchKey, final List<DeviceInformation> devices) {

        log.debug("Search result : {} -> {}", searchKey, devices);

        final LinkedList<JsonObject> result = new LinkedList<>();

        // search through all found devices ...

        for (final DeviceInformation device : devices) {

            final String tenantId = device.getTenantId();
            final String deviceId = device.getDeviceId();

            // search through all credentials of this device ...

            for (final DeviceCredential credential : device.getCredentials()) {

                final CredentialKey key = credentialKey(tenantId, credential.getAuthId(), credential.getType());

                if (!key.equals(searchKey)) {
                    log.debug("Result key doesn't match - expected: {}, actual: {}", searchKey, key);
                    // ... filter out non-matching entries
                    continue;
                }

                if (credential.getEnabled() != null && !credential.getEnabled()) {
                    log.debug("Credential is not enabled - enabled: {}", credential.getEnabled());
                    // ... filter out disabled entries for adapter
                    continue;
                }

                final var secrets = credential.getSecrets();
                if (secrets == null) {
                    log.debug("Secrets are 'null'");
                    // ... null secrets list gets filtered out
                    continue;
                }

                filterSecrets(secrets);

                if (secrets.isEmpty()) {
                    log.debug("Empty secrets list");
                    // no more secrets after filtering ... remove entry from result
                    continue;
                }

                // record matches

                result.add(Credentials.fromInternalToAdapterJson(deviceId, credential));

            }

        }

        log.debug("Search result mapping : {} -> {}", searchKey, result);

        return result;

    }

    private void filterSecrets(final List<String> secrets) {
        for (final Iterator<String> i = secrets.iterator(); i.hasNext();) {
            try {
                final JsonObject json = new JsonObject(i.next());
                if (!isValidSecret(json)) {
                    i.remove();
                }
            } catch (final Exception e) {
                log.debug("Failed to filter secret, removing...", e);
                // failed to parse
                i.remove();
            }
        }
    }

    protected boolean isValidSecret(final JsonObject secret) {
        if (!secret.getBoolean(CredentialsConstants.FIELD_ENABLED, Boolean.TRUE)) {
            return false;
        }

        return true;
    }

    private <T> CredentialsResult<T> notFound(final Duration ttl) {
        return CredentialsResult.from(
                HTTP_NOT_FOUND,
                null,
                toCacheDirective(ttl));
    }


    private CredentialsResult<JsonObject> found(final JsonObject result, final Duration ttl) {
        return CredentialsResult.from(
                HTTP_OK,
                result,
                toCacheDirective(ttl));
    }

    private static CacheDirective toCacheDirective(final Duration ttl) {

        final long seconds = ttl.toSeconds();

        final CacheDirective cacheDirective;
        if (seconds > 0) {
            cacheDirective = maxAgeDirective(seconds);
        } else {
            cacheDirective = noCacheDirective();
        }

        return cacheDirective;

    }

}
