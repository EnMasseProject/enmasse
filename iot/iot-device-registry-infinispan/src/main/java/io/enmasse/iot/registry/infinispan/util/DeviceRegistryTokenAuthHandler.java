/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.infinispan.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.HttpStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRegistryTokenAuthHandler extends AuthHandlerImpl {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);
    static final HttpStatusException BAD_REQUEST = new HttpStatusException(400);

    static final String BEARER = "Bearer";
    static final String TOKEN = "token";
    static final String ENDPOINT = "endpoint";
    static final String TENANT = "tenant";
    static final String METHOD = "method";

    public DeviceRegistryTokenAuthHandler(AuthProvider authProvider) {
        super(authProvider);
    }

    @Override
    public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
        final HttpServerRequest request = context.request();
        final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            handler.handle(Future.failedFuture(UNAUTHORIZED));
            return;
        }

        try {
            int idx = authorization.indexOf(' ');

            if (idx <= 0) {
                handler.handle(Future.failedFuture(BAD_REQUEST));
                return;
            }

            final String type = authorization.substring(0, idx);

            if (!BEARER.equals(type)) {
                handler.handle(Future.failedFuture(UNAUTHORIZED));
                return;
            }

            final String[] pathElements = request.path().split("/");
            final String endpoint = pathElements[2];
            final String tenant = pathElements[3];
            log.info("Authenticating tenant '{}' for endpoint '{}'", tenant, endpoint);
            final JsonObject authInfo = new JsonObject()
                    .put(TOKEN, authorization.substring(idx + 1))
                    .put(ENDPOINT, endpoint)
                    .put(TENANT, tenant)
                    .put(METHOD, request.method().toString());

            handler.handle(Future.succeededFuture(authInfo));
        } catch (RuntimeException e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    @Override
    protected String authenticateHeader(RoutingContext context) {
        return BEARER;
    }
}
