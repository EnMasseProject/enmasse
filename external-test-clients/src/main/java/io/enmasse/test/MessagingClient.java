/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import org.HdrHistogram.AtomicHistogram;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessagingClient {
    public static void main(String[] args) {
        String endpointHost = args[0];
        String endpointPort = args[1];
        String ca = System.getenv("CA");

        List<String> addresses = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {

        }
    }

    private static final AtomicLong connectFailures = new AtomicLong(0);

    private static class ProbeClient extends AbstractVerticle {
        private final String host;
        private final int port;

        private ProbeClient(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void start(Future<Void> startPromise) {
            ProtonClient client = ProtonClient.create(vertx);
            client.connect(host, port, connectResult -> {
                if (connectResult.succeeded()) {
                    ProtonConnection connection = connectResult.result();
                    // TODO: Record success, failure and reconnect latencies.
                } else {
                    connectFailures.incrementAndGet();
                }
            });
        }

    }
}
