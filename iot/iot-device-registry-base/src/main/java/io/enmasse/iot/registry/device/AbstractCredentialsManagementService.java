/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.device;

import static io.enmasse.iot.registry.device.DeviceKey.deviceKey;
import static io.enmasse.iot.utils.MoreFutures.finishHandler;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;

import org.eclipse.hono.auth.HonoPasswordEncoder;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.eclipse.hono.service.management.credentials.PasswordSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Throwables;

import io.enmasse.iot.registry.tenant.TenantInformationService;
import io.enmasse.iot.utils.MoreFutures;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public abstract class AbstractCredentialsManagementService implements CredentialsManagementService {

    private static final Logger log = LoggerFactory.getLogger(AbstractCredentialsManagementService.class);

    private final Vertx vertx;

    private HonoPasswordEncoder passwordEncoder;

    @Autowired
    protected TenantInformationService tenantInformationService;

    public AbstractCredentialsManagementService(final HonoPasswordEncoder passwordEncoder, final Vertx vertx) {
        this.passwordEncoder = passwordEncoder;
        this.vertx = vertx;
    }

    public void setTenantInformationService(final TenantInformationService tenantInformationService) {
        this.tenantInformationService = tenantInformationService;
    }

    @Override
    public void set(final String tenantId, final String deviceId, final Optional<String> resourceVersion, final List<CommonCredential> credentials, final Span span,
            final Handler<AsyncResult<OperationResult<Void>>> resultHandler) {
        finishHandler(() -> processSet(tenantId, deviceId, resourceVersion, credentials, span), resultHandler);
    }

    protected Future<OperationResult<Void>> processSet(final String tenantId, final String deviceId, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span) {

        return verifyAndEncodePasswords(credentials)
                .flatMap(encodedCredentials -> {
                    return this.tenantInformationService
                            .tenantExists(tenantId, HTTP_NOT_FOUND, span.context())
                            .flatMap(tenantHandle -> processSet(deviceKey(tenantHandle, deviceId), resourceVersion, encodedCredentials, span));
                })
                .recover(e -> {
                    log.info("Failed to set credentials", e);
                    if (Throwables.getRootCause(e) instanceof IllegalStateException) {
                        // An illegal state exception is actually a bad request
                        return Future.succeededFuture(OperationResult.empty(HttpURLConnection.HTTP_BAD_REQUEST));
                    } else {
                        return Future.failedFuture(e);
                    }
                });

    }

    protected abstract Future<OperationResult<Void>> processSet(DeviceKey key, Optional<String> resourceVersion, List<CommonCredential> credentials,
            Span span);

    @Override
    public void get(final String tenantId, final String deviceId, final Span span, final Handler<AsyncResult<OperationResult<List<CommonCredential>>>> resultHandler) {
        finishHandler(() -> processGet(tenantId, deviceId, span), resultHandler);
    }

    protected Future<OperationResult<List<CommonCredential>>> processGet(final String tenantId, final String deviceId, final Span span) {

        return this.tenantInformationService
                .tenantExists(tenantId, HTTP_NOT_FOUND, span.context())
                .flatMap(tenantHandle -> processGet(deviceKey(tenantHandle, deviceId), span));

    }

    protected abstract Future<OperationResult<List<CommonCredential>>> processGet(DeviceKey key, Span span);

    private Future<List<CommonCredential>> verifyAndEncodePasswords(final List<CommonCredential> credentials) {

        // Check if we need to encode passwords

        if (!needToEncode(credentials)) {
            // ... no, so don't fork off a worker task, but inline work
            try {
                return Future.succeededFuture(checkCredentials(credentials));
            } catch (Exception e) {
                return Future.failedFuture(e);
            }
        }

        return MoreFutures.executeBlocking(this.vertx, () -> checkCredentials(checkCredentials(credentials)));

    }

    /**
     * Check if we need to encode any secrets.
     *
     * @param credentials The credentials to check.
     * @return {@code true} is the list contains at least one password which needs to be encoded on the
     *         server side.
     */
    private static boolean needToEncode(final List<CommonCredential> credentials) {
        return credentials
                .stream()
                .filter(PasswordCredential.class::isInstance)
                .map(PasswordCredential.class::cast)
                .flatMap(c -> c.getSecrets().stream())
                .anyMatch(s -> s.getPasswordPlain() != null && !s.getPasswordPlain().isEmpty());
    }

    protected List<CommonCredential> checkCredentials(final List<CommonCredential> credentials) {
        for (final CommonCredential credential : credentials) {
            checkCredential(credential);
        }
        return credentials;
    }

    /**
     * Validate a secret and hash the password if necessary.
     *
     * @param credential The secret to validate.
     * @throws IllegalStateException if the secret is not valid.
     */
    private void checkCredential(final CommonCredential credential) {
        credential.checkValidity();
        if (credential instanceof PasswordCredential) {
            for (final PasswordSecret passwordSecret : ((PasswordCredential) credential).getSecrets()) {
                passwordSecret.encode(this.passwordEncoder);
                passwordSecret.checkValidity();
            }
        }
    }
}
