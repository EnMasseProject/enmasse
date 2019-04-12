/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.api.auth.AllowAllAuthInterceptor;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.AuthInterceptor;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.JacksonConfig;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.api.v1.http.*;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.user.api.UserApi;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Clock;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int SECURE_PORT = 8443;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    private final SchemaProvider schemaProvider;
    private final String certDir;
    private final String clientCa;
    private final String requestHeaderClientCa;
    private final AuthApi authApi;
    private final UserApi userApi;
    private final boolean isRbacEnabled;
    private final Clock clock;
    private final int port;

    private HttpServer httpServer;

    public HTTPServer(AddressSpaceApi addressSpaceApi,
                      SchemaProvider schemaProvider,
                      AuthApi authApi,
                      UserApi userApi,
                      ApiServerOptions options,
                      String clientCa,
                      String requestHeaderClientCa,
                      Clock clock) {
        this(addressSpaceApi,
                schemaProvider,
                authApi,
                userApi,
                options,
                clientCa,
                requestHeaderClientCa,
                clock,
                SECURE_PORT);
    }

    public HTTPServer(AddressSpaceApi addressSpaceApi,
            SchemaProvider schemaProvider,
            AuthApi authApi,
            UserApi userApi,
            ApiServerOptions options,
            String clientCa,
            String requestHeaderClientCa,
            Clock clock,
            int port) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.certDir = options.getCertDir();
        this.clientCa = clientCa;
        this.requestHeaderClientCa = requestHeaderClientCa;
        this.authApi = authApi;
        this.userApi = userApi;
        this.isRbacEnabled = options.isEnableRbac();
        this.clock = clock;
        this.port = port;
    }

    @Override
    public void start(Future<Void> startPromise) {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);
        deployment.getProviderFactory().registerProvider(JacksonConfig.class);

        if (isRbacEnabled) {
            log.info("Enabling RBAC for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AuthInterceptor(authApi, path ->
                    path.equals(HttpHealthService.BASE_URI) ||
                            path.equals(HttpMetricsService.BASE_URI) ||
                            path.equals("/swagger.json")));
        } else {
            log.info("Disabling authentication and authorization for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AllowAllAuthInterceptor());
        }

        deployment.getRegistry().addSingletonResource(new SwaggerSpecEndpoint());
        deployment.getRegistry().addSingletonResource(new HttpOpenApiService());
        deployment.getRegistry().addSingletonResource(new HttpNestedAddressService(addressSpaceApi, schemaProvider, clock));
        deployment.getRegistry().addSingletonResource(new HttpAddressService(addressSpaceApi, schemaProvider, clock));
        deployment.getRegistry().addSingletonResource(new HttpClusterAddressService(addressSpaceApi, schemaProvider, clock));
        deployment.getRegistry().addSingletonResource(new HttpSchemaService(schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpAddressSpaceService(addressSpaceApi, schemaProvider, clock));
        deployment.getRegistry().addSingletonResource(new HttpClusterAddressSpaceService(addressSpaceApi, clock));
        deployment.getRegistry().addSingletonResource(new HttpUserService(addressSpaceApi, userApi, clock));
        deployment.getRegistry().addSingletonResource(new HttpClusterUserService(userApi, clock));
        deployment.getRegistry().addSingletonResource(new HttpRootService());
        deployment.getRegistry().addSingletonResource(new HttpApiRootService());

        VertxRequestHandler vertxRequestHandler = new VertxRequestHandler(vertx, deployment);
        Handler<HttpServerRequest> requestHandler = event -> {
            log.info("Request {} {}", event.method(), event.path());
            vertxRequestHandler.handle(event);
        };

        createSecureServer(requestHandler, startPromise);
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    private void createSecureServer(Handler<HttpServerRequest> requestHandler, Future<Void> startPromise) {
        HttpServerOptions options = new HttpServerOptions();
        if (new File(certDir).exists()) {
            File keyFile = new File(certDir, "tls.key");
            File certFile = new File(certDir, "tls.crt");
            log.info("Loading key from " + keyFile.getAbsolutePath() + ", cert from " + certFile.getAbsolutePath());
            options.setKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(keyFile.getAbsolutePath())
                    .setCertPath(certFile.getAbsolutePath()));
            options.setSsl(true);

            if (clientCa != null || requestHeaderClientCa != null) {
                log.info("Enabling client authentication");
                PemTrustOptions trustOptions = new PemTrustOptions();
                if (clientCa != null) {
                    log.info("Adding client CA");
                    trustOptions.addCertValue(Buffer.buffer(clientCa));
                }

                if (requestHeaderClientCa != null) {
                    log.info("Adding request header client CA");
                    trustOptions.addCertValue(Buffer.buffer(requestHeaderClientCa));
                }

                options.setTrustOptions(trustOptions);
                options.setClientAuth(ClientAuth.REQUEST);
            }
        }

        httpServer = vertx.createHttpServer(options)
                .requestHandler(requestHandler)
                .listen(this.port, ar -> {
                    if (ar.succeeded()) {
                        int actualPort = ar.result().actualPort();
                        log.info("Started HTTPS server. Listening on port {}", actualPort);
                        startPromise.complete();
                    } else {
                        log.info("Error starting HTTPS server");
                        startPromise.fail(ar.cause());
                    }
                });
    }

    public int getActualPort() {
        return httpServer.actualPort();
    }
}
