/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.controller;

import io.enmasse.controller.api.JacksonConfig;
import io.enmasse.controller.api.osb.v2.BasicAuthInterceptor;
import io.enmasse.controller.api.osb.v2.OSBServiceBase;
import io.enmasse.controller.api.osb.v2.bind.OSBBindingService;
import io.enmasse.controller.api.osb.v2.catalog.OSBCatalogService;
import io.enmasse.controller.api.osb.v2.lastoperation.OSBLastOperationService;
import io.enmasse.controller.api.osb.v2.provision.OSBProvisioningService;
import io.enmasse.controller.api.v1.http.HttpAddressService;
import io.enmasse.controller.api.v1.http.HttpSchemaService;
import io.enmasse.controller.common.exceptionmapping.DefaultExceptionMapper;
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
import java.net.PasswordAuthentication;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int PORT = 8080;
    public static final int SECURE_PORT = 8081;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    private final String certDir;
    private final Optional<PasswordAuthentication> osbAuth;

    private HttpServer httpServer;
    private HttpServer httpsServer;

    public HTTPServer(AddressSpaceApi addressSpaceApi, String certDir, Optional<PasswordAuthentication> osbAuth) {
        this.addressSpaceApi = addressSpaceApi;
        this.certDir = certDir;
        this.osbAuth = osbAuth;
    }

    @Override
    public void start(Future<Void> startPromise) {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);
        deployment.getProviderFactory().registerProvider(JacksonConfig.class);
        osbAuth.ifPresent(auth -> deployment.getProviderFactory().registerProviderInstance(new BasicAuthInterceptor(auth, OSBServiceBase.BASE_URI)));

        deployment.getRegistry().addSingletonResource(new HttpAddressService(addressSpaceApi));
        deployment.getRegistry().addSingletonResource(new HttpSchemaService());
        //deployment.getRegistry().addSingletonResource(new HttpAddressSpaceService(addressSpaceApi));

        deployment.getRegistry().addSingletonResource(new OSBCatalogService(addressSpaceApi));
        deployment.getRegistry().addSingletonResource(new OSBProvisioningService(addressSpaceApi));
        deployment.getRegistry().addSingletonResource(new OSBBindingService(addressSpaceApi));
        deployment.getRegistry().addSingletonResource(new OSBLastOperationService(addressSpaceApi));

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
            // TODO: Remove once Vert.x supports PKCS#1: https://github.com/eclipse/vert.x/issues/1851
            // This also implies that _KEY ROTATION DOES NOT WORK_
            File outputFile = new File("/tmp/pkcs8.key");
            convertKey(keyFile, outputFile);

            File certFile = new File(certDir, "tls.crt");
            log.info("Loading key from " + keyFile.getAbsolutePath() + ", cert from " + certFile.getAbsolutePath());
            options.setKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(outputFile.getAbsolutePath())
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

    private void convertKey(File keyFile, File outputFile) {
        try {
            Process p = new ProcessBuilder("openssl", "pkcs8", "-topk8", "-inform", "PEM",
                    "-outform", "PEM", "-nocrypt", "-in", keyFile.getAbsolutePath(), "-out", outputFile.getAbsolutePath())
                    .start();
            p.waitFor(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.info("Error converting key from PKCS#1 to PKCS#8");
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
