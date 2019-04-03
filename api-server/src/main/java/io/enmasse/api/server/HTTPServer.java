/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.api.auth.AllowAllAuthInterceptor;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.AuthInterceptor;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
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
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Clock;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int PORT = 8080;
    public static final int SECURE_PORT = 8443;
    private static final int PROCESS_LINE_BUFFER_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final AddressSpaceApi addressSpaceApi;
    private final SchemaProvider schemaProvider;
    private final String certDir;
    private final String clientCa;
    private final String requestHeaderClientCa;
    private final AuthApi authApi;
    private final UserApi userApi;
    private final Metrics metrics;
    private final boolean isRbacEnabled;
    private final String version;
    private final Clock clock;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final int port;
    private final int securePort;

    private HttpServer httpServer;
    private HttpServer httpsServer;

    public HTTPServer(AddressSpaceApi addressSpaceApi,
            SchemaProvider schemaProvider,
            AuthApi authApi,
            UserApi userApi,
            Metrics metrics,
            ApiServerOptions options,
            String clientCa,
            String requestHeaderClientCa,
            Clock clock,
            AuthenticationServiceRegistry authenticationServiceRegistry) {
        this(addressSpaceApi,
                schemaProvider,
                authApi,
                userApi,
                metrics,
                options,
                clientCa,
                requestHeaderClientCa,
                clock,
                authenticationServiceRegistry,
                PORT,
                SECURE_PORT);
    }

    public HTTPServer(AddressSpaceApi addressSpaceApi,
            SchemaProvider schemaProvider,
            AuthApi authApi,
            UserApi userApi,
            Metrics metrics,
            ApiServerOptions options,
            String clientCa,
            String requestHeaderClientCa,
            Clock clock,
            AuthenticationServiceRegistry authenticationServiceRegistry,
            int port,
            int securePort) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
        this.metrics = metrics;
        this.certDir = options.getCertDir();
        this.clientCa = clientCa;
        this.requestHeaderClientCa = requestHeaderClientCa;
        this.authApi = authApi;
        this.userApi = userApi;
        this.isRbacEnabled = options.isEnableRbac();
        this.version = options.getVersion();
        this.clock = clock;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
        this.port = port;
        this.securePort = securePort;
    }

    @Override
    public void start(Future<Void> startPromise) {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);

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
        deployment.getRegistry().addSingletonResource(new HttpAddressSpaceService(addressSpaceApi, schemaProvider, clock, authenticationServiceRegistry));
        deployment.getRegistry().addSingletonResource(new HttpClusterAddressSpaceService(addressSpaceApi, clock));
        deployment.getRegistry().addSingletonResource(new HttpUserService(addressSpaceApi, userApi, authenticationServiceRegistry, clock));
        deployment.getRegistry().addSingletonResource(new HttpClusterUserService(userApi, authenticationServiceRegistry, clock));
        deployment.getRegistry().addSingletonResource(new HttpHealthService());
        deployment.getRegistry().addSingletonResource(new HttpMetricsService(version, metrics));
        deployment.getRegistry().addSingletonResource(new HttpRootService());
        deployment.getRegistry().addSingletonResource(new HttpApiRootService());

        VertxRequestHandler vertxRequestHandler = new VertxRequestHandler(vertx, deployment);
        Handler<HttpServerRequest> requestHandler = event -> {
            log.info("Request {} {}", event.method(), event.path());
            vertxRequestHandler.handle(event);
        };

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

    private void createSecureServer(Handler<HttpServerRequest> requestHandler, Future<Void> startPromise) {
        if (new File(certDir).exists()) {
            HttpServerOptions options = new HttpServerOptions();
            File keyFile = new File(certDir, "tls.key");
            if (!keyFile.exists()) {
                keyFile = new File(certDir, "apiserver.key");
            }
            File certFile = new File(certDir, "tls.crt");
            if (!certFile.exists()) {
                certFile = new File(certDir, "apiserver.crt");
            }

            log.info("Loading key from " + keyFile.getAbsolutePath() + ", cert from " + certFile.getAbsolutePath());
            runCommand("openssl", "pkcs12", "-export", "-passout", "pass:enmasse", "-in", certFile.getAbsolutePath(), "-inkey", keyFile.getAbsolutePath(), "-name", "server", "-out", "/tmp/cert.p12");
            runCommand("keytool", "-importkeystore", "-srcstorepass", "enmasse", "-deststorepass", "enmasse", "-destkeystore", "/tmp/keystore.jks", "-srckeystore", "/tmp/cert.p12", "-srcstoretype", "PKCS12");
            options.setKeyCertOptions(new JksOptions()
                    .setPassword("enmasse")
                    .setPath("/tmp/keystore.jks"));
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

            httpsServer = vertx.createHttpServer(options)
                    .requestHandler(requestHandler)
                    .listen(this.securePort, ar -> {
                        if (ar.succeeded()) {
                            int actualPort = ar.result().actualPort();
                            log.info("Started HTTPS server. Listening on port {}", actualPort);
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

    private void createOpenServer(Handler<HttpServerRequest> requestHandler, Future<Void> startPromise) {
        httpServer = vertx.createHttpServer()
                .requestHandler(requestHandler)
                .listen(this.port, ar -> {
                    if (ar.succeeded()) {
                        int actualPort = ar.result().actualPort();
                        log.info("Started HTTP server. Listening on port {}", actualPort);
                        startPromise.complete();
                    } else {
                        log.info("Error starting HTTP server");
                        startPromise.fail(ar.cause());
                    }
                });
    }

    public int getActualSecurePort() {
        return httpsServer.actualPort();
    }

    public int getActualPort() {
        return httpServer.actualPort();
    }

    private static void runCommand(String... cmd) {
        ProcessBuilder keyGenBuilder = new ProcessBuilder(cmd).redirectErrorStream(true);

        log.info("Running command '{}'", keyGenBuilder.command());
        Deque<String> outBuf = new LinkedBlockingDeque<>(PROCESS_LINE_BUFFER_SIZE);
        boolean success = false;
        try {
            Process process = keyGenBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    boolean added = outBuf.offerLast(line);
                    if (log.isDebugEnabled()) {
                        log.debug("Command output: {}", line);
                    }
                    if (!added) {
                        outBuf.removeFirst();
                        outBuf.addLast(line);
                    }
                }
            }
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                throw new RuntimeException(String.format("Command '%s' timed out", keyGenBuilder.command()));
            }

            final int exitValue = process.waitFor();
            success = exitValue == 0;
            String msg = String.format("Command '%s' completed with exit value %d", keyGenBuilder.command(), exitValue);
            if (success) {
                log.info(msg);
            } else {
                log.error(msg);
                throw new RuntimeException(String.format("Command '%s' failed with exit value %d", keyGenBuilder.command(), exitValue));
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!success && !outBuf.isEmpty()) {
                log.error("Last {} line(s) written by command to stdout/stderr follow", outBuf.size());
                outBuf.forEach(line -> log.error("Command output: {}", line));
            }
        }
    }
}
