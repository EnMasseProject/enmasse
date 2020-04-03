/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_MANAGEMENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.eclipse.hono.service.management.OperationResult.empty;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.util.CacheDirective;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.enmasse.iot.jdbc.store.device.AbstractDeviceManagementStore;
import io.enmasse.iot.registry.device.AbstractCredentialsManagementService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.enmasse.iot.registry.jdbc.config.DeviceServiceProperties;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

@Component
@Profile(PROFILE_REGISTRY_MANAGEMENT)
public class CredentialsManagementServiceImpl extends AbstractCredentialsManagementService {

    private final AbstractDeviceManagementStore store;
    private final Optional<CacheDirective> ttl;

    @Autowired
    public CredentialsManagementServiceImpl(final Vertx vertx, final HonoPasswordEncoder passwordEncoder, final AbstractDeviceManagementStore store, final DeviceServiceProperties properties) {
        super(passwordEncoder, vertx);
        this.store = store;
        this.ttl = Optional.of(CacheDirective.maxAgeDirective(properties.getCredentialsTtl().toSeconds()));
    }

    @Override
    protected Future<OperationResult<Void>> processSet(final DeviceKey key, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span) {

        return this.store
                .setCredentials(key, credentials, resourceVersion, span.context())
                .<OperationResult<Void>>map(r -> {
                    if (Boolean.TRUE.equals(r)) {
                        return empty(HTTP_NO_CONTENT);
                    } else {
                        return empty(HTTP_NOT_FOUND);
                    }
                });

    }

    @Override
    protected Future<OperationResult<List<CommonCredential>>> processGet(final DeviceKey key, final Span span) {

        return this.store.getCredentials(key, span.context())
                .<OperationResult<List<CommonCredential>>>map(r -> {

                    if (r.isPresent()) {
                        var result = r.get();
                        return OperationResult.ok(
                                HTTP_OK,
                                result.getCredentials(),
                                this.ttl,
                                result.getResourceVersion());
                    } else {
                        return empty(HTTP_NOT_FOUND);
                    }

                })

                .otherwise(err -> empty(HttpURLConnection.HTTP_INTERNAL_ERROR));

    }


}
