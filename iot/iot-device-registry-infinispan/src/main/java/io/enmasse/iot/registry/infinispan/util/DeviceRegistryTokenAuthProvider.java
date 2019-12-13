/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.infinispan.util;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.auth.TokenReview;
import static io.enmasse.iot.registry.infinispan.util.DeviceRegistryTokenAuthHandler.METHOD;
import static io.enmasse.iot.registry.infinispan.util.DeviceRegistryTokenAuthHandler.TENANT;
import static io.enmasse.iot.registry.infinispan.util.DeviceRegistryTokenAuthHandler.TOKEN;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.handler.impl.HttpStatusException;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DeviceRegistryTokenAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryTokenAuthProvider.class);

    private static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);

    private final NamespacedKubernetesClient client;
    private final AuthApi authApi;
    private final EmbeddedCacheManager cacheManager = new DefaultCacheManager();
    private Cache<String, TokenReview> tokens;
    private Cache<String, Boolean> authorizations;

    public DeviceRegistryTokenAuthProvider(Duration tokenExpiration) {
        log.info("Using token cache expiration of {}", tokenExpiration);
        this.client = new DefaultKubernetesClient();
        this.authApi = new KubeAuthApi(this.client, this.client.getConfiguration().getOauthToken());
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.expiration().lifespan(tokenExpiration.toSeconds(), TimeUnit.SECONDS);
        cacheManager.defineConfiguration("tokens", config.build());
        cacheManager.defineConfiguration("subjects", config.build());
        tokens = cacheManager.getCache("tokens");
        authorizations = cacheManager.getCache("subjects");

    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

        final String token = authInfo.getString(TOKEN);
        tokens.computeIfAbsentAsync(token, review -> authApi.performTokenReview(token))
                .exceptionally(t -> {
                    log.info("Error performing token review", t);
                    return null;
                })
                .thenAccept(tokenReview -> {
                    if (tokenReview != null && tokenReview.isAuthenticated()) {
                        String role = getRole(authInfo);
                        if (role != null) {
                            RbacSecurityContext securityContext = new RbacSecurityContext(tokenReview, authApi, null);
                            authorizations
                                    .computeIfAbsentAsync(role, authorized -> securityContext.isUserInRole(role))
                                    .exceptionally(t-> {
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
                        } else {
                            log.debug("Error parsing the role from {}", authInfo);
                            resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
                        }
                    } else {
                        log.debug("Bearer token not authenticated");
                        resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
                    }
                });
    }

    private String getRole(JsonObject authInfo) {
        final HttpMethod method = HttpMethod.valueOf(authInfo.getString(METHOD));
        ResourceVerb verb = ResourceVerb.update;
        if (method == HttpMethod.GET) {
            verb = ResourceVerb.get;
        }

        final String[] tenant = authInfo.getString(TENANT).split("\\.");
        if (tenant.length != 2) {
            log.info("Wrong tenant format (namespace.project) for value '{}'", tenant);
            return null;
        }
        final String namespace = tenant[0];
        final String iotProject = tenant[1];

        return RbacSecurityContext.rbacToRole(namespace, verb, "iotprojects", iotProject, "iot.enmasse.io");
    }

}