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
import io.enmasse.metrics.api.MetricLabel;
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
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MessagingClient extends AbstractVerticle {
    private static final AtomicLong connectSuccesses = new AtomicLong(0);
    private static final AtomicLong connectFailures = new AtomicLong(0);
    private static final AtomicLong disconnects = new AtomicLong(0);
    private static final AtomicLong reconnects = new AtomicLong(0);

    private static final Map<AddressType, Histogram> reconnectTime = Map.of(
            AddressType.anycast, new AtomicHistogram(Long.MAX_VALUE, 2),
            AddressType.queue, new AtomicHistogram(Long.MAX_VALUE, 2));

    private static final Map<AddressType, Histogram> reattachTime = Map.of(
            AddressType.anycast, new AtomicHistogram(Long.MAX_VALUE, 2),
            AddressType.queue, new AtomicHistogram(Long.MAX_VALUE, 2));

    private static final Map<AddressType, AtomicLong> numAccepted = Map.of(
            AddressType.anycast, new AtomicLong(0),
            AddressType.queue, new AtomicLong(0));

    private static final Map<AddressType, AtomicLong> numRejected = Map.of(
            AddressType.anycast, new AtomicLong(0),
            AddressType.queue, new AtomicLong(0));

    private static final Map<AddressType, AtomicLong> numModified = Map.of(
            AddressType.anycast, new AtomicLong(0),
            AddressType.queue, new AtomicLong(0));

    private static final Map<AddressType, AtomicLong> numReleased = Map.of(
            AddressType.anycast, new AtomicLong(0),
            AddressType.queue, new AtomicLong(0));

    private static final double percentile = 99.0;
    private static final Metrics metrics = new Metrics();

    static {
        metrics.registerMetric(new ScalarMetric("enmasse_test_connect_success_total", "Connect success",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(connectSuccesses.get()))));
        metrics.registerMetric(new ScalarMetric("enmasse_test_connect_failure_total", "Connect failure",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(connectFailures.get()))));
        metrics.registerMetric(new ScalarMetric("enmasse_test_disconnects_total", "Disconnects",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(disconnects.get()))));
        metrics.registerMetric(new ScalarMetric("enmasse_test_reconnects_total", "Reconnects",
                MetricType.counter,
                () -> Collections.singletonList(new MetricValue(reconnects.get()))));

        metrics.registerMetric(new ScalarMetric("enmasse_test_reconnect_duration_millis_99p", "Reconnect duration 99p",
                MetricType.gauge,
                () -> reconnectTime.entrySet().stream()
                        .map(entry -> new MetricValue(TimeUnit.NANOSECONDS.toMillis(entry.getValue().getValueAtPercentile(percentile)), new MetricLabel("addressType", entry.getKey().name())))
                        .collect(Collectors.toList())));

        metrics.registerMetric(new ScalarMetric("enmasse_test_reattach_duration_millis_99p", "Reattach duration 99p",
                MetricType.gauge,
                () -> reattachTime.entrySet().stream()
                        .map(entry -> new MetricValue(TimeUnit.NANOSECONDS.toMillis(entry.getValue().getValueAtPercentile(percentile)), new MetricLabel("addressType", entry.getKey().name())))
                        .collect(Collectors.toList())));

        metrics.registerMetric(new ScalarMetric("enmasse_test_accepted_total", "Accepted messages",
                MetricType.counter,
                () -> numAccepted.entrySet().stream()
                        .map(entry -> new MetricValue(entry.getValue().get(), new MetricLabel("addressType", entry.getKey().name())))
                        .collect(Collectors.toList())));

        metrics.registerMetric(new ScalarMetric("enmasse_test_rejected_total", "Rejected messages",
                MetricType.counter,
                () -> numRejected.entrySet().stream()
                        .map(entry -> new MetricValue(entry.getValue().get(), new MetricLabel("addressType", entry.getKey().name())))
                        .collect(Collectors.toList())));

        metrics.registerMetric(new ScalarMetric("enmasse_test_released_total", "Released messages",
                MetricType.counter,
                () -> numReleased.entrySet().stream()
                        .map(entry -> new MetricValue(entry.getValue().get(), new MetricLabel("addressType", entry.getKey().name())))
                        .collect(Collectors.toList())));

        metrics.registerMetric(new ScalarMetric("enmasse_test_modified_total", "Modified messages",
                MetricType.counter,
                () -> numModified.entrySet().stream()
                        .map(entry -> new MetricValue(entry.getValue().get(), new MetricLabel("addressType", entry.getKey().name())))
                        .collect(Collectors.toList())));

    }

    private final String host;
    private final int port;
    private final AddressType addressType;
    private final LinkType linkType;
    private final List<String> addresses;
    private final Map<String, AtomicLong> lastDetach = new HashMap<>();
    private final AtomicLong lastDisconnect = new AtomicLong(0);

    private MessagingClient(String host, int port, AddressType addressType, LinkType linkType, List<String> addresses) {
        this.host = host;
        this.port = port;
        this.addressType = addressType;
        this.linkType = linkType;
        this.addresses = new ArrayList<>(addresses);
        for (String address : addresses) {
            lastDetach.put(address, new AtomicLong(0));
        }
    }

    @Override
    public void start(Future<Void> startPromise) {
        ProtonClient client = ProtonClient.create(vertx);
        connectAndAttach(client, startPromise, 1);
    }

    private static final long maxRetryDelay = 5000;

    private void connectAndAttach(ProtonClient client, Future<Void> startPromise, long retryDelay) {
        ProtonClientOptions protonClientOptions = new ProtonClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setHostnameVerificationAlgorithm("");

        client.connect(protonClientOptions, host, port, connectResult -> {
            if (connectResult.succeeded()) {
                ProtonConnection connection = connectResult.result();

                // We've been reconnected. Record how long it took
                if (lastDisconnect.get() > 0) {
                    reconnectTime.get(addressType).recordValue(System.nanoTime() - lastDisconnect.get());
                }

                connection.closeHandler(closeResult -> {
                    lastDisconnect.set(System.nanoTime());
                    vertx.setTimer(retryDelay, id -> {
                        connectAndAttach(client, null, Math.min(retryDelay * 2, maxRetryDelay));
                    });
                });

                for (String address : addresses) {
                    attachLink(connection, address, retryDelay);
                }
                connection.open();
            } else {
                connectFailures.incrementAndGet();
                if (startPromise != null) {
                    startPromise.fail(connectResult.cause());
                } else {
                    vertx.setTimer(retryDelay, handler -> {
                        connectAndAttach(client, null, Math.max(retryDelay * 2, maxRetryDelay));
                    });
                }
            }
        });
    }

    private void attachLink(ProtonConnection connection, String address, long retryDelay) {
        if (linkType.equals(LinkType.receiver)) {
            ProtonReceiver receiver = connection.createReceiver(address);
            receiver.handler((protonDelivery, message) -> {
                connection.close();
            });
            receiver.openHandler(receiverAttachResult -> {
                if (receiverAttachResult.succeeded()) {
                    // We've been reattached. Record how long it took
                    if (lastDetach.get(address).get() > 0) {
                        reattachTime.get(addressType).recordValue(System.nanoTime() - lastDetach.get(address).get());
                    }
                }
            });
            receiver.closeHandler(closeResult -> {
                lastDetach.get(address).set(System.nanoTime());
                vertx.setTimer(retryDelay, id -> {
                    attachLink(connection, address, Math.min(retryDelay * 2, maxRetryDelay));
                });
            });
            receiver.open();
        } else {
            ProtonSender sender = connection.createSender(address);
            sender.openHandler(senderAttachResult -> {
                if (senderAttachResult.succeeded()) {
                    // We've been reattached. Record how long it took
                    if (lastDetach.get(address).get() > 0) {
                        reattachTime.get(addressType).recordValue(System.nanoTime() - lastDetach.get(address).get());
                    }
                    sendMessage(sender);
                }
            });
            sender.open();
        }
    }

    private void sendMessage(ProtonSender sender) {
        Message message = Proton.message();
        message.setBody(new AmqpValue("HELLO"));
        if (addressType.equals(AddressType.queue)) {
            message.setDurable(true);
        }
        sender.send(message, delivery -> {
            switch (delivery.getRemoteState().getType()) {
                case Accepted:
                    numAccepted.get(addressType).incrementAndGet();
                    break;
                case Rejected:
                    numRejected.get(addressType).incrementAndGet();
                    break;
                case Modified:
                    numModified.get(addressType).incrementAndGet();
                    break;
                case Released:
                    numReleased.get(addressType).incrementAndGet();
                    break;
            }
            if (delivery.remotelySettled()) {
                vertx.setTimer(500, id -> {
                    sendMessage(sender);
                });
            }
        });
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 5) {
            System.err.println("Usage: java -jar messaging-client.jar <kubernetes api url> <kubernetes api token> <address namespace> <address space> <number of addresses> <links per connection>");
            System.exit(1);
        }
        String masterUrl = args[0];
        String token = args[1];
        String namespace = args[2];
        String addressSpaceName = args[3];
        int numAddresses = Integer.parseInt(args[4]);
        int linksPerConnection = Integer.parseInt(args[5]);

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

        UUID instanceId = UUID.randomUUID();
        // Attempt to clean up after ourselves
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            addressClient.withLabel("instance", instanceId.toString()).delete();
        }));

        List<Address> anycastAddresses = new ArrayList<>();
        List<Address> queueAddresses = new ArrayList<>();


        for (int i = 0; i < addresses.size(); i++) {
            String address = addresses.get(i);
            String name = String.format("%s.%s", addressSpace, address);
            final Address resource = new AddressBuilder()
                    .editOrNewMetadata()
                    .withName(name)
                    .addToLabels("client", "messaging-client")
                    .addToLabels("app", "test-clients")
                    .addToLabels("instance", instanceId.toString())
                    .endMetadata()
                    .editOrNewSpec()
                    .withAddress(address)
                    .withType(i % 2 == 0 ? "anycast" : "queue")
                    .withPlan(i % 2 == 0 ? "standard-small-anycast" : "standard-small-queue")
                    .endSpec()
                    .build();
            addressClient.createOrReplace(resource);
            if (i % 2 == 0) {
                anycastAddresses.add(resource);
            } else {
                queueAddresses.add(resource);
            }
        }

        MetricsServer metricsServer = new MetricsServer(8080, metrics);
        metricsServer.start();

        Vertx vertx = Vertx.vertx();

        deployClients(vertx, endpointHost, endpointPort, AddressType.anycast, linksPerConnection, anycastAddresses);
        deployClients(vertx, endpointHost, endpointPort, AddressType.queue, linksPerConnection, queueAddresses);

        MetricsFormatter formatter = new ConsoleFormatter();
        while (true) {
            Thread.sleep(30000);
            System.out.println(formatter.format(metrics.getMetrics(), 0));
        }
    }

    private static void deployClients(Vertx vertx, String endpointHost, int endpointPort, AddressType addressType, int linksPerConnection, List<Address> addresses) {
        List<List<Address>> groups = new ArrayList<>();
        for (int i = 0; i < addresses.size() / linksPerConnection; i++) {
            groups.add(addresses.subList(i * linksPerConnection, (i + 1) * linksPerConnection));
        }

        for (List<Address> group : groups) {
            List<String> addressList = group.stream().map(a -> a.getSpec().getAddress()).collect(Collectors.toList());
            vertx.deployVerticle(new MessagingClient(endpointHost, endpointPort, addressType, LinkType.receiver, addressList), result -> {
                if (result.succeeded()) {
                    System.out.println("Started receiver client for addresses " + addressList);
                } else {
                    System.out.println("Failed starting receiver client for addresses " + addressList);
                }
            });

            vertx.deployVerticle(new MessagingClient(endpointHost, endpointPort, addressType, LinkType.sender, addressList), result -> {
                if (result.succeeded()) {
                    System.out.println("Started sender client for addresses " + addressList);
                } else {
                    System.out.println("Failed starting sender client for addresses " + addressList);
                }
            });
        }

    }
}
