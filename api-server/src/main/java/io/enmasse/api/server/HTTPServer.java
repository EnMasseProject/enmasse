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
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.api.v1.http.*;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int PORT = 8080;
    public static final int SECURE_PORT = 8443;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    private final SchemaProvider schemaProvider;
    private final String certDir;
    private final String clientCa;
    private final AuthApi authApi;

    private HttpServer httpServer;
    private HttpServer httpsServer;

    public HTTPServer(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, String certDir, String clientCa, AuthApi authApi) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.certDir = certDir;
        this.clientCa = clientCa;
        this.authApi = authApi;
    }

    @Override
    public void start(Future<Void> startPromise) {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);
        deployment.getProviderFactory().registerProvider(JacksonConfig.class);

        if (authApi != null) {
            log.info("Enabling RBAC for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AuthInterceptor(authApi, path ->
                    path.equals(HttpHealthService.BASE_URI) ||
                    path.equals("/swagger.json") ||
                    path.equals("/apis") ||
                    path.equals("/apis/enmasse.io") ||
                    path.equals("/apis/enmasse.io/v1alpha1")));
        } else {
            log.info("Disabling authentication and authorization for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AllowAllAuthInterceptor());
        }

        deployment.getRegistry().addSingletonResource(new SwaggerSpecEndpoint());
        deployment.getRegistry().addSingletonResource(new HttpNestedAddressService(addressSpaceApi, schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpAddressService(addressSpaceApi, schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpSchemaService(schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpAddressSpaceService(addressSpaceApi, schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpHealthService());
        deployment.getRegistry().addSingletonResource(new HttpRootService());
        deployment.getRegistry().addSingletonResource(new HttpApiRootService());

        VertxRequestHandler requestHandler = new VertxRequestHandler(vertx, deployment);

        Future<Void> secureReady = Future.future();
        Future<Void> openReady = Future.future();
        CompositeFuture readyFuture = CompositeFuture.all(secureReady, openReady);
        readyFuture.setHandler(result -> {
            if (result.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });

        createSecureServer(requestHandler, secureReady);
        createOpenServer(requestHandler, openReady);
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.close();
        }
        if (httpsServer != null) {
            httpsServer.close();
        }
    }

    private void createSecureServer(VertxRequestHandler requestHandler, Future<Void> startPromise) {
        if (new File(certDir).exists()) {
            HttpServerOptions options = new HttpServerOptions();
            File keyFile = new File(certDir, "tls.key");
            File certFile = new File(certDir, "tls.crt");
            log.info("Loading key from " + keyFile.getAbsolutePath() + ", cert from " + certFile.getAbsolutePath());
            options.setKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(keyFile.getAbsolutePath())
                    .setCertPath(certFile.getAbsolutePath()));
            options.setSsl(true);

            if (clientCa != null) {
                log.info("Enabling client authentication");
                options.setTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(clientCa)));
                options.setClientAuth(ClientAuth.REQUEST);
            }

            httpsServer = vertx.createHttpServer(options)
                    .requestHandler(requestHandler)
                    .listen(SECURE_PORT, ar -> {
                        if (ar.succeeded()) {
                            log.info("Started HTTPS server. Listening on port " + SECURE_PORT);
                            startPromise.complete();
                        } else {
                            log.info("Error starting HTTPS server");
                            startPromise.fail(ar.cause());
                        }
                    });
        } else {
            startPromise.complete();
        }
    }

    private void createOpenServer(VertxRequestHandler requestHandler, Future<Void> startPromise) {
        httpServer = vertx.createHttpServer()
                .requestHandler(requestHandler)
                .listen(PORT, ar -> {
                    if (ar.succeeded()) {
                        log.info("Started HTTP server. Listening on port " + PORT);
                        startPromise.complete();
                    } else {
                        log.info("Error starting HTTP server");
                        startPromise.fail(ar.cause());
                    }
                });
    }
}
