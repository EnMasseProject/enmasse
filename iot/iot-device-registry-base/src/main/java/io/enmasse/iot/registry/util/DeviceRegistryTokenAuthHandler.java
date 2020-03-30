/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.util;

import static io.enmasse.iot.utils.MoreFutures.finishHandler;

import org.eclipse.hono.service.http.TracingHandler;
import org.eclipse.hono.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
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

public class DeviceRegistryTokenAuthHandler extends AuthHandlerImpl {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryTokenAuthHandler.class);

    private static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);
    private static final HttpStatusException BAD_REQUEST = new HttpStatusException(400);

    static final String BEARER = "Bearer";
    static final String TOKEN = "token";
    static final String ENDPOINT = "endpoint";
    static final String TENANT = "tenant";
    static final String METHOD = "method";

    private final Tracer tracer;

    public DeviceRegistryTokenAuthHandler(final Tracer tracer, final AuthProvider authProvider) {
        super(authProvider);
        this.tracer = tracer;
    }

    @Override
    public void parseCredentials(final RoutingContext context, final Handler<AsyncResult<JsonObject>> handler) {
        finishHandler(() -> processParseCredentials(context), handler);
    }

    protected Future<JsonObject> processParseCredentials(final RoutingContext context) {

        final SpanContext spanContext = TracingHandler.serverSpanContext(context);

        final HttpServerRequest request = context.request();
        final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            return Future.failedFuture(UNAUTHORIZED);
        }

        try {
            int idx = authorization.indexOf(' ');

            if (idx <= 0) {
                return Future.failedFuture(BAD_REQUEST);
            }

            final String type = authorization.substring(0, idx);

            if (!BEARER.equals(type)) {
                return Future.failedFuture(UNAUTHORIZED);
            }

            final String[] pathElements = request.path().split("/");
            final String endpoint = pathElements[2];
            final String tenant = pathElements[3];
            log.debug("Authenticating tenant '{}' for endpoint '{}'", tenant, endpoint);
            final JsonObject authInfo = new JsonObject()
                    .put(TOKEN, authorization.substring(idx + 1))
                    .put(ENDPOINT, endpoint)
                    .put(TENANT, tenant)
                    .put(METHOD, request.method().toString());

            if (spanContext != null) {
                TracingHelper.injectSpanContext(this.tracer, spanContext, authInfo);
            }

            return Future.succeededFuture(authInfo);
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    @Override
    protected String authenticateHeader(RoutingContext context) {
        return BEARER;
    }
}
