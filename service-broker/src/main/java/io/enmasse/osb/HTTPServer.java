/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.osb;

import io.enmasse.api.auth.AllowAllAuthInterceptor;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.AuthInterceptor;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.JacksonConfig;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.osb.api.bind.OSBBindingService;
import io.enmasse.osb.api.catalog.OSBCatalogService;
import io.enmasse.osb.api.console.HttpConsoleService;
import io.enmasse.osb.api.lastoperation.OSBLastOperationService;
import io.enmasse.osb.api.provision.OSBProvisioningService;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.osb.keycloak.KeycloakApi;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HTTPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    private final AuthApi authApi;
    private final String certDir;
    private final boolean enableRbac;
    private final SchemaProvider schemaProvider;
    private final KeycloakApi keycloakApi;
    private final int listenPort;
    private final String consolePrefix;

    private HttpServer httpServer;

    public HTTPServer(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider,
                      AuthApi authApi, String certDir, boolean enableRbac,
                      KeycloakApi keycloakApi, int listenPort, String consolePrefix) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.certDir = certDir;
        this.authApi = authApi;
        this.enableRbac = enableRbac;
        this.keycloakApi = keycloakApi;
        this.listenPort = listenPort;
        this.consolePrefix = consolePrefix;
    }

    @Override
    public void start(Future<Void> startPromise) {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);
        deployment.getProviderFactory().registerProvider(JacksonConfig.class);

        if (enableRbac) {
            log.info("Enabling RBAC for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AuthInterceptor(authApi, HttpHealthService.BASE_URI, HttpConsoleService.BASE_URI));
        } else {
            log.info("Disabling authentication and authorization for REST API");
            deployment.getProviderFactory().registerProviderInstance(new AllowAllAuthInterceptor());
        }

        deployment.getRegistry().addSingletonResource(new HttpHealthService());
        deployment.getRegistry().addSingletonResource(new HttpConsoleService(addressSpaceApi));
        deployment.getRegistry().addSingletonResource(new OSBCatalogService(addressSpaceApi, authApi, schemaProvider));
        deployment.getRegistry().addSingletonResource(new OSBProvisioningService(addressSpaceApi, authApi, schemaProvider, consolePrefix));
        deployment.getRegistry().addSingletonResource(new OSBBindingService(addressSpaceApi, authApi, schemaProvider, keycloakApi));
        deployment.getRegistry().addSingletonResource(new OSBLastOperationService(addressSpaceApi, authApi, schemaProvider));

        VertxRequestHandler requestHandler = new VertxRequestHandler(vertx, deployment);

        httpServer = createServer(requestHandler, startPromise);
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    int getActualPort() {
        if (httpServer != null) {
            return httpServer.actualPort();
        } else {
            return listenPort;
        }
    }

    private HttpServer createServer(VertxRequestHandler requestHandler, Future<Void> startPromise) {
        if (certDir != null && new File(certDir).exists()) {
            HttpServerOptions options = new HttpServerOptions();
            File keyFile = new File(certDir, "tls.key");
            File certFile = new File(certDir, "tls.crt");
            log.info("Loading key from " + keyFile.getAbsolutePath() + ", cert from " + certFile.getAbsolutePath());
            options.setKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(keyFile.getAbsolutePath())
                    .setCertPath(certFile.getAbsolutePath()));
            options.setSsl(true);

            return vertx.createHttpServer(options)
                    .requestHandler(requestHandler)
                    .listen(listenPort, ar -> {
                        if (ar.succeeded()) {
                            log.info("Started HTTPS server. Listening on port " + listenPort);
                            startPromise.complete();
                        } else {
                            log.info("Error starting HTTPS server");
                            startPromise.fail(ar.cause());
                        }
                    });
        } else {
            return vertx.createHttpServer()
                    .requestHandler(requestHandler)
                    .listen(listenPort, ar -> {
                        if (ar.succeeded()) {
                            log.info("Started HTTP server. Listening on port " + listenPort);
                            startPromise.complete();
                        } else {
                            log.info("Error starting HTTP server");
                            startPromise.fail(ar.cause());
                        }
                    });
        }
    }
}
