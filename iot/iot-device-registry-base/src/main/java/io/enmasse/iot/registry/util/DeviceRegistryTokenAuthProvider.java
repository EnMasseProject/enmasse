/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.util;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.auth.TokenReview;
import io.enmasse.iot.registry.tenant.TenantInformation;
import io.enmasse.iot.registry.tenant.TenantInformationService;
import static io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler.METHOD;
import static io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler.TENANT;
import static io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler.TOKEN;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DeviceRegistryTokenAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryTokenAuthProvider.class);

    private static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);

    protected TenantInformationService tenantInformationService;

    private final NamespacedKubernetesClient client;
    private final AuthApi authApi;
    private final EmbeddedCacheManager cacheManager = new DefaultCacheManager();
    private final Cache<String, TokenReview> tokens;
    private final Cache<String, Boolean> authorizations;

    public DeviceRegistryTokenAuthProvider(final Duration tokenExpiration) {
        log.info("Using token cache expiration of {}", tokenExpiration);
        this.client = new DefaultKubernetesClient();
        this.authApi = new KubeAuthApi(this.client, this.client.getConfiguration().getOauthToken());
        final ConfigurationBuilder config = new ConfigurationBuilder();
        config.expiration().lifespan(tokenExpiration.toSeconds(), TimeUnit.SECONDS);
        this.cacheManager.defineConfiguration("tokens", config.build());
        this.cacheManager.defineConfiguration("subjects", config.build());
        this.tokens = cacheManager.getCache("tokens");
        this.authorizations = cacheManager.getCache("subjects");

    }

    @Autowired
    public void setTenantInformationService(final TenantInformationService tenantInformationService) {
        this.tenantInformationService = tenantInformationService;
    }

    @Override
    public void authenticate(final JsonObject authInfo, final Handler<AsyncResult<User>> resultHandler) {

        final String token = authInfo.getString(TOKEN);
        this.tokens.computeIfAbsentAsync(token, review -> authApi.performTokenReview(token))
                .exceptionally(t -> {
                    log.info("Error performing token review", t);
                    return null;
                })
                .thenAccept(tokenReview -> {
                    if (tokenReview != null && tokenReview.isAuthenticated()) {
                        tenantInformationService.tenantExists(authInfo.getString(TENANT), HTTP_NOT_FOUND, NoopSpan.INSTANCE)
                                .exceptionally(e -> {
                                    log.info("Tenant doesn't exists", e);
                                    resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
                                    return null;
                                })
                                .thenAccept(tenant -> authorize(authInfo, tokenReview, tenant, resultHandler)
                                );
                    } else {
                        log.debug("Bearer token not authenticated");
                        resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
                    }
                });
    }

    private void authorize(final JsonObject authInfo, final TokenReview tokenReview, final TenantInformation tenant, final Handler<AsyncResult<User>> resultHandler) {
        final HttpMethod method = HttpMethod.valueOf(authInfo.getString(METHOD));
        ResourceVerb verb = ResourceVerb.update;
        if (method == HttpMethod.GET) {
            verb = ResourceVerb.get;
        }
        String role = RbacSecurityContext.rbacToRole(tenant.getNamespace(), verb, "iotprojects", tenant.getProjectName(), "iot.enmasse.io");
        RbacSecurityContext securityContext = new RbacSecurityContext(tokenReview, authApi, null);
        authorizations
                .computeIfAbsentAsync(role, authorized -> securityContext.isUserInRole(role))
                .exceptionally(t -> {
                    log.info("Error performing authorization", t);
                    return false;
                })
                .thenAccept(authResult -> {
                    if (authResult) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        log.debug("Bearer token not authorized");
                        resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
                    }
                });
    }
}