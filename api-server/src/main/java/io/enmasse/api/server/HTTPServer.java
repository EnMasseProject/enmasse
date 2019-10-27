/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.vertx.core.AbstractVerticle;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * HTTP server for deploying address config
 */
public class HTTPServer extends AbstractVerticle {
    public static final int SECURE_PORT = 8443;
    private static final int PROCESS_LINE_BUFFER_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(HTTPServer.class.getName());
    private final String certDir;
    private final String clientCa;
    private final String requestHeaderClientCa;
    private final ResteasyDeploymentFactory resteasyDeploymentFactory;

    private final int port;

    private HttpServer httpServer;

    public HTTPServer(ApiServerOptions options,
                      ResteasyDeploymentFactory resteasyDeploymentFactory,
                      String clientCa,
                      String requestHeaderClientCa) {
        this(options, resteasyDeploymentFactory, clientCa, requestHeaderClientCa, SECURE_PORT);
    }

    public HTTPServer(ApiServerOptions options,
            ResteasyDeploymentFactory resteasyDeploymentFactory,
            String clientCa,
            String requestHeaderClientCa,
            int port) {
        this.certDir = options.getCertDir();
        this.clientCa = clientCa;
        this.requestHeaderClientCa = requestHeaderClientCa;
        this.port = port;
        this.resteasyDeploymentFactory = resteasyDeploymentFactory;
    }

    @Override
    public void start(Future<Void> startPromise) {

        VertxRequestHandler vertxRequestHandler = new VertxRequestHandler(vertx, resteasyDeploymentFactory.getInstance());

        createSecureServer(vertxRequestHandler, startPromise);
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
