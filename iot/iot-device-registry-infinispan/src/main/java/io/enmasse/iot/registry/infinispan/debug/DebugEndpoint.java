/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.debug;

import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;

import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.service.registration.RegistrationService;
import org.eclipse.hono.util.RequestResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.enmasse.iot.service.base.ServiceBase;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Component
@ConditionalOnProperty("debug")
@ConfigurationProperties(ServiceBase.CONFIG_BASE + ".registry.debug")
public class DebugEndpoint extends AbstractVerticle {

    @Autowired
    private CredentialsService crendentialService;

    @Autowired
    private RegistrationService registrationService;

    private int port = 0; // ephemeral port by default

    public void setCrendentialService(final CredentialsService crendentialService) {
        this.crendentialService = crendentialService;
    }

    public void setRegistrationService(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void start(final Future<Void> future) {

        var router = Router
                .router(this.vertx);

        router
                .get("/debug/credentials/:tenant/:type/:authId")
                .handler(ctx -> {

                    var tenantId = ctx.pathParam("tenant");
                    var type = ctx.pathParam("type");
                    var authId = ctx.pathParam("authId");

                    this.crendentialService.get(tenantId, type, authId, ar -> {
                        handleResult(ctx, ar);
                    });
                });

        router
                .get("/debug/registration/:tenant/:device")
                .handler(ctx -> {

                    var tenantId = ctx.pathParam("tenant");
                    var deviceId = ctx.pathParam("device");

                    this.registrationService.assertRegistration(tenantId, deviceId, ar -> {
                        handleResult(ctx, ar);
                    });
                });

        this.vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(this.port, ar -> {
                    if (ar.succeeded()) {
                        System.err.format("Debug server listening on port %s", ar.result().actualPort());
                        future.complete();
                    } else {
                        ar.cause().printStackTrace();
                        future.fail(ar.cause());
                    }
                });

    }

    private void handleResult(RoutingContext ctx, AsyncResult<? extends RequestResponseResult<JsonObject>> ar) {
        if (ar.failed()) {
            ctx.fail(500, ar.cause());
        } else {

            final var cache = ar.result().getCacheDirective();
            final var result = ar.result().getPayload();

            if (cache != null) {
                ctx.response()
                        .putHeader(CACHE_CONTROL, cache.toString());
            }

            ctx.response()
                    .setStatusCode(ar.result().getStatus());

            if (result != null) {
                ctx.response().end(Json.encode(result));
            } else {
                ctx.response().end();
            }
        }
    }

}
