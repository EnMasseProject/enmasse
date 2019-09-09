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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRegistryTokenAuthProvider implements AuthProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);

    private final NamespacedKubernetesClient client;
    private final AuthApi authApi;

    public DeviceRegistryTokenAuthProvider() {
        this.client = new DefaultKubernetesClient();
        this.authApi = new KubeAuthApi(this.client, this.client.getConfiguration().getOauthToken());
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

        final String token = authInfo.getString(TOKEN);

        final HttpMethod method = HttpMethod.valueOf(authInfo.getString(METHOD));
        ResourceVerb verb = ResourceVerb.update;
        if (method == HttpMethod.GET) {
            verb = ResourceVerb.get;
        }

        final String[] tenant = authInfo.getString(TENANT).split("\\.");
        if (tenant.length != 2) {
            resultHandler.handle(Future.failedFuture(new HttpStatusException(HTTP_BAD_REQUEST,
                    new JsonObject()
                            .put("error", "Tenant in wrong format: namespace.project")
                            .toString())));
            return;
        }
        final String namespace = tenant[0];
        final String iotProject = tenant[1];

        TokenReview review = authApi.performTokenReview(token);
        log.info("Authorizing token for '{}' access to '{}' iot project in '{}' namespace", verb, iotProject, namespace);
        if (review.isAuthenticated()) {
            RbacSecurityContext securityContext = new RbacSecurityContext(review, authApi, null);
            if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "iotprojects", iotProject, "iot.enmasse.io"))) {
                log.info("Bearer token not authorized");
                resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
            }
            // TODO It's possible to cache authorities in the user object implementation based on
            // review.getUserName()
            resultHandler.handle(Future.succeededFuture());
        } else {
            log.info("Bearer token not authenticated");
            resultHandler.handle(Future.failedFuture(UNAUTHORIZED));
        }
    }
}