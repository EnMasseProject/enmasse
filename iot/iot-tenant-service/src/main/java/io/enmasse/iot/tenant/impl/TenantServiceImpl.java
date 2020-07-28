/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.util.Strings;
import org.eclipse.hono.util.TenantConstants;
import org.eclipse.hono.util.TenantResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.enmasse.iot.model.v1.IoTTenant;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TenantServiceImpl extends AbstractTenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantServiceImpl.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<X500Principal, IoTTenant> tenantsByTrustAnchor = new HashMap<>();
    private final Map<String, Set<X500Principal>> tenantToTrustAnchors = new HashMap<>();

    @Override
    public Future<TenantResult<JsonObject>> get(final String tenantName, final Span span) {

        logger.debug("Get tenant - name: {}", tenantName);

        span.log(ImmutableMap.<String, Object>builder()
                .put("event", "get tenant")
                .put("tenant_id", tenantName)
                .build());

        return getTenant(tenantName)

                .map(tenant -> tenant
                        .map(p -> convertToHono(p, span.context()))
                        .orElse(RESULT_NOT_FOUND));

    }

    @Override
    protected void onAdd(final IoTTenant tenant) {
        super.onAdd(tenant);

        final String key = key(tenant);
        final Set<X500Principal> trustAnchors = anchors(tenant);

        writing(() -> {

            // we add a mapping for each subject dn to the tenant

            for (X500Principal subjectDn : trustAnchors) {
                this.tenantsByTrustAnchor.put(subjectDn, tenant);
            }

            // and add a link from the tenant to the trust anchors

            this.tenantToTrustAnchors.put(key, trustAnchors);

            logger.info("Mapped {} -> {}", key, trustAnchors);

        });

    }

    @Override
    protected void onDelete(final IoTTenant tenant) {

        super.onDelete(tenant);

        // the tenant content might already be gone, so only use the key

        final String key = key(tenant);

        writing(() -> {

            // we remove and get the mapped trust anchors for this tenant

            final Set<X500Principal> trustAnchors = this.tenantToTrustAnchors.remove(key);

            logger.info("Unmapped {} <- {}", key, trustAnchors);

            // and remove all trust anchors

            if (trustAnchors != null) {
                trustAnchors.forEach(this.tenantsByTrustAnchor::remove);
            }

        });

    }

    @Override
    protected void onUpdate(IoTTenant tenant) {

        super.onUpdate(tenant);

        // the tenant content might already be gone, so only use the key and the new trust anchors
        final String key = key(tenant);
        final Set<X500Principal> newTrustAnchors = anchors(tenant);

        writing(() -> {

            // we remove and get the mapped trust anchors for this tenant

            final Set<X500Principal> oldTrustAnchors = this.tenantToTrustAnchors.remove(key);

            // and delete all old trust anchors

            if (oldTrustAnchors != null) {
                oldTrustAnchors.forEach(this.tenantsByTrustAnchor::remove);
            }

            // now we re-add all new trust anchor mappings to the tenant

            for (X500Principal subjectDn : newTrustAnchors) {
                this.tenantsByTrustAnchor.put(subjectDn, tenant);
            }

            // and also re-add the mapping from the tenant to the mapped trust anchors

            this.tenantToTrustAnchors.put(key, newTrustAnchors);

            logger.info("Re-mapped {} -> {} -> {}", key, oldTrustAnchors, newTrustAnchors);

        });

    }

    private Set<X500Principal> anchors(final IoTTenant project) {

        if (project == null || project.getStatus() == null
                || project.getStatus().getAccepted() == null
                || project.getStatus().getAccepted().getConfiguration() == null) {

            // no configuration yet
            return Collections.emptySet();

        }

        final JsonObject config = JsonObject.mapFrom(project.getStatus().getAccepted().getConfiguration());
        final JsonArray trustAnchors = config.getJsonArray(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA);
        if (trustAnchors == null) {
            return Collections.emptySet();
        }

        final Set<X500Principal> result = new HashSet<>();

        for (Object value : trustAnchors) {
            if (!(value instanceof JsonObject)) {
                continue;
            }
            final JsonObject trustAnchor = (JsonObject) value;
            if (!trustAnchor.getBoolean(TenantConstants.FIELD_ENABLED, true)) {
                continue;
            }
            final String subjectDn = trustAnchor.getString(TenantConstants.FIELD_PAYLOAD_SUBJECT_DN);
            if (Strings.isNullOrEmpty(subjectDn)) {
                continue;
            }

            result.add(new X500Principal(subjectDn));
        }

        return result;
    }

    @Override
    public Future<TenantResult<JsonObject>> get(final X500Principal subjectDn, final Span span) {

        logger.debug("Get tenant - subject DN: {}", subjectDn);

        span.log(ImmutableMap.<String, Object>builder()
                .put("event", "get tenant")
                .put("subject_dn", subjectDn)
                .build());

        // get the project

        final Optional<IoTTenant> project =
                reading(() -> ofNullable(this.tenantsByTrustAnchor.get(subjectDn)));
        logger.debug("Result of get: {}", project);

        // convert and return the result

        return Future.succeededFuture(project)
                .map(p -> p
                        .map(p2 -> convertToHono(p2, span.context()))
                        .orElse(RESULT_NOT_FOUND));

    }

    /**
     * Call the runnable, holding the write lock.
     *
     * @param runnable The runnable to call.
     */
    private void writing(final Runnable runnable) {
        final Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            runnable.run();
        } catch (Exception e) {
            logger.warn("Failed to perform trust anchor update", e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Call the supplier, holding the read lock.
     *
     * @param <T> Type of the return value.
     * @param supplier The supplier to call, while holding the read lock.
     * @return The result of the supplier.
     */
    private <T> T reading(final Supplier<T> supplier) {
        final Lock lock = this.lock.readLock();
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

}
