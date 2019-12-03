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
import io.vertx.core.Vertx;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

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
import java.util.concurrent.TimeUnit;

public class DeviceRegistryTokenAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryTokenAuthProvider.class);

    private static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);

    private final NamespacedKubernetesClient client;
    private final AuthApi authApi;
    private final Vertx vertx;
    private final EmbeddedCacheManager cacheManager = new DefaultCacheManager();
    private Cache<String, TokenReview> tokens;
    private Cache<String, Boolean> authorizations;

    public DeviceRegistryTokenAuthProvider(Vertx vertx, int tokenExpiration) {
        log.info("Using token cache expiration of {}", tokenExpiration);
        this.vertx = vertx;
        this.client = new DefaultKubernetesClient();
        this.authApi = new KubeAuthApi(this.client, this.client.getConfiguration().getOauthToken());
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.expiration().lifespan(tokenExpiration, TimeUnit.SECONDS);
        cacheManager.defineConfiguration("tokens", config.build());
        cacheManager.defineConfiguration("subjects", config.build());
        tokens = cacheManager.getCache("tokens");
        authorizations = cacheManager.getCache("subjects");

    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

        final String token = authInfo.getString(TOKEN);

        performTokenReview(token, tokenResult -> {
            if (tokenResult.failed()) {
                resultHandler.handle(Future.failedFuture(tokenResult.cause()));
            }
            TokenReview review = tokenResult.result();
            if (review.isAuthenticated()) {
                String role = getRole(authInfo, review, authApi);
                RbacSecurityContext securityContext = new RbacSecurityContext(review, authApi, null);
                performAuthorization(role, securityContext, authResult -> {
                    if (authResult.failed()) {
                         resultHandler.handle(Future.failedFuture(authResult.cause()));
                    }
                    if (!authResult.result()) {
                        log.debug("Bearer token not authorized");
                        resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
                    } else {
                        resultHandler.handle(Future.succeededFuture());
                    }

                });
            } else {
                log.debug("Bearer token not authenticated");
                resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
            }

        });
    }

    private void performTokenReview(String token, Handler<AsyncResult<TokenReview>> resultHandler) {
        tokens.getAsync(token).thenAccept(review -> {
           if (review == null) {
               log.debug("Authenticating token {}", token);
               vertx.<TokenReview>executeBlocking(call -> {
                   call.complete(authApi.performTokenReview(token));
               }, res -> {
                   if (res.succeeded()) {
                       TokenReview reviewResult = res.result();
                       tokens.putAsync(token, reviewResult).thenRun(() -> {
                           resultHandler.handle(Future.succeededFuture(reviewResult));
                       });
                   } else {
                       resultHandler.handle(Future.failedFuture(res.cause()));
                   }
               });
           } else {
               resultHandler.handle(Future.succeededFuture(review));
           }
        });
    }

    private void performAuthorization(String role, RbacSecurityContext securityContext, Handler<AsyncResult<Boolean>> resultHandler) {
        authorizations.getAsync(role).thenAccept(authorized -> {
            if (authorized == null) {
                log.debug("Authorizing role {}", role);
                vertx.<Boolean>executeBlocking(call -> {
                    call.complete(securityContext.isUserInRole(role));
                }, res -> {
                        if (res.succeeded()) {
                            Boolean auth = res.result();
                            authorizations.putAsync(role, auth).thenRun(() -> {
                                resultHandler.handle(Future.succeededFuture(auth));
                            });
                        } else {
                            resultHandler.handle(Future.failedFuture(res.cause()));
                        }
                });
            } else {
                resultHandler.handle(Future.succeededFuture(authorized));
            }
        });
    }


    private String getRole(JsonObject authInfo, TokenReview review, AuthApi authApi) throws HttpStatusException {
        final HttpMethod method = HttpMethod.valueOf(authInfo.getString(METHOD));
        ResourceVerb verb = ResourceVerb.update;
        if (method == HttpMethod.GET) {
            verb = ResourceVerb.get;
        }

        final String[] tenant = authInfo.getString(TENANT).split("\\.");
        if (tenant.length != 2) {
            throw new HttpStatusException(HTTP_BAD_REQUEST,
                    new JsonObject()
                            .put("error", "Tenant in wrong format: namespace.project")
                            .toString());
        }
        final String namespace = tenant[0];
        final String iotProject = tenant[1];

        return RbacSecurityContext.rbacToRole(namespace, verb, "iotprojects", iotProject, "iot.enmasse.io");
    }

}