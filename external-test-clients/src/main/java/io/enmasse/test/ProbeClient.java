/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

import io.enmasse.metrics.api.MetricType;
import io.enmasse.metrics.api.MetricValue;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.metrics.api.MetricsFormatter;
import io.enmasse.metrics.api.ScalarMetric;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class ProbeClient extends AbstractVerticle {
    private final String host;
    private final int port;
    private final String address;

    private ProbeClient(String host, int port, String address) {
        this.host = host;
        this.port = port;
        this.address = address;
    }

    @Override
    public void start(Future<Void> startPromise) {
        ProtonClientOptions protonClientOptions = new ProtonClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setHostnameVerificationAlgorithm("");

        ProtonClient client = ProtonClient.create(vertx);
        client.connect(protonClientOptions, host, port, connectResult -> {
            if (connectResult.succeeded()) {
                ProtonConnection connection = connectResult.result();
                ProtonReceiver receiver = connection.createReceiver(address);
                receiver.handler((protonDelivery, message) -> {
                    startPromise.complete();
                    connection.close();
                });
                receiver.openHandler(receiverAttachResult -> {
                    if (receiverAttachResult.succeeded()) {
                        ProtonSender sender = connection.createSender(address);
                        sender.openHandler(senderAttachResult -> {
                            if (senderAttachResult.succeeded()) {
                                Message message = Proton.message();
                                message.setBody(new AmqpValue("PING"));
                                sender.send(message);
                            } else {
                                startPromise.fail(senderAttachResult.cause());
                            }
                        });
                        sender.open();
                    } else {
                        startPromise.fail(receiverAttachResult.cause());
                    }
                });
                receiver.open();
                connection.open();
            } else {
                startPromise.fail(connectResult.cause());
            }
        });
    }

    private static final AtomicLong probeSuccesses = new AtomicLong(0);
    private static final AtomicLong probeFailures = new AtomicLong(0);
    private static final Metrics metrics = new Metrics();

    static {
        metrics.registerMetric(new ScalarMetric("enmasse_test_probe_success_total", "Probe success",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(probeSuccesses.get()))));
        metrics.registerMetric(new ScalarMetric("enmasse_test_probe_failure_total", "Probe failure",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(probeFailures.get()))));
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        String endpointHost = args[0];
        int endpointPort = Integer.parseInt(args[1]);

        List<String> addresses = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            addresses.add(args[i]);
        }

        MetricsServer metricsServer = new MetricsServer(8080, metrics);
        metricsServer.start();

        Vertx vertx = Vertx.vertx();
        MetricsFormatter formatter = new ConsoleFormatter();
        while (true) {
            CountDownLatch completed = new CountDownLatch(addresses.size());
            for (String address : addresses) {
                vertx.deployVerticle(new ProbeClient(endpointHost, endpointPort, address), result -> {
                    if (result.succeeded()) {
                        probeSuccesses.incrementAndGet();
                    } else {
                        probeFailures.incrementAndGet();
                    }
                    completed.countDown();
                });
            }
            completed.await();
            Thread.sleep(10000);
            formatter.format(metrics.getMetrics(), 0);
        }
    }
}
