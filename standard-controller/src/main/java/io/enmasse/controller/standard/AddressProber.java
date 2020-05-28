/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AddressProber {
    private static final Logger log = LoggerFactory.getLogger(AddressProber.class);
    private final Vertx vertx;
    private final String containerId;
    private final ProtonClientOptions options;
    private final Duration probeTimeout;

    public AddressProber(Vertx vertx, String containerId, ProtonClientOptions options, Duration probeTimeout) {
        this.vertx = vertx;
        this.containerId = containerId;
        this.options = options;
        this.probeTimeout = probeTimeout;
    }

    public static AddressProber withCertsInDir(Vertx vertx, String containerId, Duration queryTimeout, String certDir) {
        ProtonClientOptions clientOptions = new ProtonClientOptions()
                .setSsl(true)
                .addEnabledSaslMechanism("EXTERNAL")
                .setHostnameVerificationAlgorithm("")
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                        .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        return new AddressProber(vertx, containerId, clientOptions, queryTimeout);
    }

    public Set<String> run(String host, int port, Set<String> addresses) throws Exception {
        Duration timeLeft = Duration.from(probeTimeout);
        try (AddressProbeClient client = new AddressProbeClient(vertx, containerId)) {
            CompletableFuture<Void> connected = new CompletableFuture<>();

            long now = System.nanoTime();
            log.debug("Connecting probe runner to {}:{} within {} seconds", host, port, timeLeft.getSeconds());
            client.connect(host, port, options, connected);
            connected.get(timeLeft.getSeconds(), TimeUnit.SECONDS);

            timeLeft = timeLeft.minus(Duration.ofNanos(System.nanoTime() - now));
            log.debug("Connected! Send/receive messages for {} within {} seconds", addresses, timeLeft.getSeconds());
            return client.probeAddresses(addresses, timeLeft);
        }
    }
}
