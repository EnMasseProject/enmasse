/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import io.enmasse.controller.api.JacksonConfig;
import io.enmasse.controller.api.AuthInterceptor;
import io.enmasse.controller.api.v1.http.SwaggerSpecEndpoint;
import io.enmasse.controller.api.v1.http.HttpAddressService;
import io.enmasse.controller.api.v1.http.HttpAddressSpaceService;
import io.enmasse.controller.api.v1.http.HttpHealthService;
import io.enmasse.controller.api.v1.http.HttpSchemaService;
import io.enmasse.controller.api.v1.http.*;
import io.enmasse.controller.api.DefaultExceptionMapper;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
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
    public static final int SECURE_PORT = 8081;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    private final SchemaProvider schemaProvider;
    private final String certDir;
    private final Kubernetes kubernetes;
    private final boolean enableRbac;

    private HttpServer httpServer;
    private HttpServer httpsServer;

    public HTTPServer(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, String certDir, Kubernetes kubernetes, boolean enableRbac) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.certDir = certDir;
        this.kubernetes = kubernetes;
        this.enableRbac = enableRbac;
    }

    @Override
    public void start(Future<Void> startPromise) {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);
        deployment.getProviderFactory().registerProvider(JacksonConfig.class);

        if (enableRbac) {
            log.info("Enabling RBAC for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AuthInterceptor(kubernetes));
        } else {
            log.info("Disabling authentication and authorization for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AllowAllAuthInterceptor());
        }

        deployment.getRegistry().addSingletonResource(new SwaggerSpecEndpoint());
        deployment.getRegistry().addSingletonResource(new HttpAddressService(addressSpaceApi, schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpSchemaService(schemaProvider));
        deployment.getRegistry().addSingletonResource(new HttpAddressSpaceService(addressSpaceApi, schemaProvider, kubernetes.getNamespace()));
        deployment.getRegistry().addSingletonResource(new HttpHealthService());
        deployment.getRegistry().addSingletonResource(new HttpV1RootService());
        deployment.getRegistry().addSingletonResource(new HttpRootService());
        deployment.getRegistry().addSingletonResource(new HttpAddressRootService(addressSpaceApi));

        //deployment.getRegistry().addSingletonResource(new OSBCatalogService(addressSpaceApi, kubernetes.getNamespace()));
        //deployment.getRegistry().addSingletonResource(new OSBProvisioningService(addressSpaceApi, kubernetes.getNamespace()));
        //deployment.getRegistry().addSingletonResource(new OSBBindingService(addressSpaceApi, kubernetes.getNamespace()));
        //deployment.getRegistry().addSingletonResource(new OSBLastOperationService(addressSpaceApi, kubernetes.getNamespace()));

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
