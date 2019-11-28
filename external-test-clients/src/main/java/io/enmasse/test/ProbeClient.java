/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.metrics.api.MetricType;
import io.enmasse.metrics.api.MetricValue;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.metrics.api.MetricsFormatter;
import io.enmasse.metrics.api.ScalarMetric;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
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
import java.util.UUID;
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
        if (args.length < 5) {
            System.err.println("Usage: java -jar probe-client.jar <kubernetes api url> <kubernetes api token> <address namespace> <address space> <number of addresses>");
            System.exit(1);
        }
        String masterUrl = args[0];
        String token = args[1];
        String namespace = args[2];
        String addressSpaceName = args[3];
        int numAddresses = Integer.parseInt(args[4]);

        NamespacedKubernetesClient client = new DefaultKubernetesClient(new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withOauthToken(token)
                .build());

        // Get endpoint info
        var addressSpaceClient = client.customResources(CoreCrd.addressSpaces(), AddressSpace.class, AddressSpaceList.class, DoneableAddressSpace.class).inNamespace(namespace);
        AddressSpace addressSpace = addressSpaceClient.withName(addressSpaceName).get();
        String endpointHost = "";
        int endpointPort = 0;
        for (EndpointStatus status : addressSpace.getStatus().getEndpointStatuses()) {
            if (status.getName().equals("messaging")) {
                endpointHost = status.getExternalHost();
                endpointPort = status.getExternalPorts().get("amqps");
            }
        }

        List<String> addresses = new ArrayList<>();
        for (int i = 0; i < numAddresses; i++) {
            addresses.add(UUID.randomUUID().toString());
        }

        var addressClient = client.customResources(CoreCrd.addresses(), Address.class, AddressList.class, DoneableAddress.class).inNamespace(namespace);
        List<Address> createdAddresses = new ArrayList<>();

        // Attempt to clean up after ourselves
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Address address : createdAddresses) {
                addressClient.delete(address);
            }
        }));

        for (int i = 0; i < addresses.size(); i++) {
            String address = addresses.get(i);
            String name = String.format("%s.%s", addressSpace, address);
            final Address resource = new AddressBuilder()
                    .editOrNewMetadata()
                    .withName(name)
                    .addToLabels("app", "probe-client")
                    .endMetadata()
                    .editOrNewSpec()
                    .withAddress(address)
                    .withType(i % 2 == 0 ? "anycast" : "queue")
                    .withPlan(i % 2 == 0 ? "standard-small-anycast" : "standard-small-queue")
                    .endSpec()
                    .build();
            addressClient.createOrReplace(resource);
            createdAddresses.add(resource);
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
