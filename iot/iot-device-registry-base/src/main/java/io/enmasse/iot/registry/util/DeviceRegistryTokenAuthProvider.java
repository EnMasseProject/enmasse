/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.registry.util;

import io.enmasse.iot.registry.tenant.KubernetesTenantInformationService;
import static io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler.METHOD;
import static io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler.TENANT;
import static io.enmasse.iot.registry.util.DeviceRegistryTokenAuthHandler.TOKEN;
import static io.enmasse.iot.utils.MoreFutures.finishHandler;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.hono.tracing.TracingHelper;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.auth.TokenReview;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.registry.tenant.TenantInformation;
import io.enmasse.iot.utils.MoreFutures;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopSpan;
import io.opentracing.tag.Tags;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.handler.impl.HttpStatusException;

public class DeviceRegistryTokenAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryTokenAuthProvider.class);

    private static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);

    private static final String IOT_PROJECT_PLURAL = IoTCrd.project().getSpec().getNames().getPlural();

    private final Tracer tracer;
    private final NamespacedKubernetesClient client;
    private final AuthApi authApi;
    private final EmbeddedCacheManager cacheManager = new DefaultCacheManager();
    private final Cache<String, TokenReview> tokens;
    private final Cache<String, Boolean> authorizations;

    //TODO Revisit this when upgrading to Hono 1.3 - https://github.com/EnMasseProject/enmasse/issues/4341
    protected KubernetesTenantInformationService tenantInformationService;

    public DeviceRegistryTokenAuthProvider(final Tracer tracer, final Duration tokenExpiration) {
        log.info("Using token cache expiration of {}", tokenExpiration);
        this.tracer = tracer;
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
    public void setTenantInformationService(final KubernetesTenantInformationService tenantInformationService) {
        this.tenantInformationService = tenantInformationService;
    }

    @Override
    public void authenticate(final JsonObject authInfo, final Handler<AsyncResult<User>> resultHandler) {
        finishHandler(() -> processAuthenticate(authInfo), resultHandler);
    }

    protected Future<User> processAuthenticate(final JsonObject authInfo) {

        log.debug("Authenticating: {}", authInfo);

        final Span span = extractSpan(authInfo, "authenticate");

        final String token = authInfo.getString(TOKEN);
        var f = this.tokens
                .computeIfAbsentAsync(token, review -> {
                    span.log("cache miss");
                    final Span childSpan = TracingHelper.buildChildSpan(this.tracer, span.context(), "perform token review", getClass().getSimpleName())
                            .start();
                    try {
                        return this.authApi.performTokenReview(token);
                    } finally {
                        childSpan.finish();
                    }
                });

        var f1 = MoreFutures.map(f)
                .flatMap(tokenReview -> {
                    span.log("eval result");
                    if (tokenReview != null && tokenReview.isAuthenticated()) {
                        return this.tenantInformationService
                                .tenantExists(authInfo.getString(TENANT), HTTP_UNAUTHORIZED, span)
                                .flatMap(tenant -> authorize(authInfo, tokenReview, tenant, span));
                    } else {
                        log.debug("Bearer token not authenticated");
                        TracingHelper.logError(span, "Bearer token not authenticated");
                        return Future.failedFuture(UNAUTHORIZED);
                    }
                });

        return MoreFutures
                .whenComplete(f1, span::finish);
    }

    private Span extractSpan(final JsonObject authInfo, final String operationName) {
        final Span span;
        final SpanContext context = TracingHelper.extractSpanContext(this.tracer, authInfo);
        if (context != null) {
            span = TracingHelper
                    .buildChildSpan(this.tracer, context, operationName, getClass().getSimpleName())
                    .start();
        } else {
            span = NoopSpan.INSTANCE;
        }
        return span;
    }

    private Future<User> authorize(final JsonObject authInfo, final TokenReview tokenReview, final TenantInformation tenant, final Span parentSpan) {

        final HttpMethod method = HttpMethod.valueOf(authInfo.getString(METHOD));
        ResourceVerb verb = ResourceVerb.update;
        if (method == HttpMethod.GET) {
            verb = ResourceVerb.get;
        }

        final Span span = TracingHelper.buildChildSpan(this.tracer, parentSpan.context(), "check user roles", getClass().getSimpleName())
                .withTag("namespace", tenant.getNamespace())
                .withTag("name", tenant.getProjectName())
                .withTag("tenant.name", tenant.getName())
                .withTag(Tags.HTTP_METHOD.getKey(), method.name())
                .start();

        final String role = RbacSecurityContext.rbacToRole(tenant.getNamespace(), verb, IOT_PROJECT_PLURAL, tenant.getProjectName(), IoTCrd.GROUP);
        final RbacSecurityContext securityContext = new RbacSecurityContext(tokenReview, this.authApi, null);
        var f = this.authorizations
                .computeIfAbsentAsync(role, authorized -> {
                    span.log("cache miss");
                    return securityContext.isUserInRole(role);
                });

        var f2 = MoreFutures.map(f)
                .otherwise(t -> {
                    log.info("Error performing authorization", t);
                    TracingHelper.logError(span, t);
                    return false;
                })
                .<User>flatMap(authResult -> {
                    if (authResult) {
                        return Future.succeededFuture();
                    } else {
                        log.debug("Bearer token not authorized");
                        TracingHelper.logError(span, "Bearer token not authorized");
                        return Future.failedFuture(UNAUTHORIZED);
                    }
                });

        return MoreFutures
                .whenComplete(f2, span::finish);
    }
}
