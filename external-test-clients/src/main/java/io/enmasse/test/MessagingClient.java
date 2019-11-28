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
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MessagingClient extends AbstractVerticle {
    private static final Counter connectSuccesses = Counter.build()
            .name("test_connect_success_total")
            .register();

    private static final Counter connectFailures = Counter.build()
            .name("test_connect_failure_total")
            .register();

    private static final Counter disconnects = Counter.build()
            .name("test_disconnects_total")
            .register();

    private static final Counter reconnects = Counter.build()
            .name("test_disconnects_total")
            .register();

    private static final Map<AddressType, Histogram> reconnectTime = Map.of(
            AddressType.anycast, new AtomicHistogram(Long.MAX_VALUE, 2),
            AddressType.queue, new AtomicHistogram(Long.MAX_VALUE, 2));

    private static final io.prometheus.client.Histogram reconnectHist = io.prometheus.client.Histogram.build()
            .name("test_reconnect_duration")
            .buckets(1.0, 2.5, 7.5, 10.0, 25.0, 50.0, 75.0, 100.0)
            .register();

    private static final Map<AddressType, Histogram> reattachTime = Map.of(
            AddressType.anycast, new AtomicHistogram(Long.MAX_VALUE, 2),
            AddressType.queue, new AtomicHistogram(Long.MAX_VALUE, 2));

    private static final io.prometheus.client.Histogram reattachHist = io.prometheus.client.Histogram.build()
            .name("test_reattach_duration")
            .labelNames("addressType")
            .buckets(1.0, 2.5, 7.5, 10.0, 25.0, 50.0, 75.0, 100.0)
            .register();


    private static final Counter numAccepted = Counter.build()
            .name("test_accepted_total")
            .labelNames("addressType")
            .register();

    private static final Counter numRejected = Counter.build()
            .name("test_rejected_total")
            .labelNames("addressType")
            .register();

    private static final Counter numReleased = Counter.build()
            .name("test_released_total")
            .labelNames("addressType")
            .register();

    private static final Counter numModified = Counter.build()
            .name("test_modified_total")
            .labelNames("addressType")
            .register();

    private static final double percentile = 99.0;

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
                connectSuccesses.inc();
                ProtonConnection connection = connectResult.result();

                // We've been reconnected. Record how long it took
                if (lastDisconnect.get() > 0) {
                    long duration = System.nanoTime() - lastDisconnect.get();
                    reconnectTime.get(addressType).recordValue(TimeUnit.NANOSECONDS.toMillis(duration));
                    reconnectHist.labels(addressType.name()).observe(toSeconds(duration));
                }

                connection.closeHandler(closeResult -> {
                    disconnects.inc();
                    lastDisconnect.set(System.nanoTime());
                    vertx.setTimer(retryDelay, id -> {
                        reconnects.inc();
                        connectAndAttach(client, null, Math.min(retryDelay * 2, maxRetryDelay));
                    });
                });

                for (String address : addresses) {
                    attachLink(connection, address, retryDelay);
                }
                connection.open();
            } else {
                connectFailures.inc();
                if (startPromise != null) {
                    startPromise.fail(connectResult.cause());
                } else {
                    vertx.setTimer(retryDelay, handler -> {
                        reconnects.inc();
                        connectAndAttach(client, null, Math.max(retryDelay * 2, maxRetryDelay));
                    });
                }
            }
        });
    }

    private static double toSeconds(long nanos) {
        return (double)nanos / 1_000_000_000.0;
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
                        long duration = System.nanoTime() - lastDetach.get(address).get();
                        reattachTime.get(addressType).recordValue(TimeUnit.NANOSECONDS.toMillis(duration));
                        reattachHist.labels(addressType.name()).observe(toSeconds(duration));
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
                        long duration = System.nanoTime() - lastDetach.get(address).get();
                        reattachTime.get(addressType).recordValue(TimeUnit.NANOSECONDS.toMillis(duration));
                        reattachHist.labels(addressType.name()).observe(toSeconds(duration));
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
                    numAccepted.labels(addressType.name()).inc();
                    break;
                case Rejected:
                    numRejected.labels(addressType.name()).inc();
                    break;
                case Modified:
                    numModified.labels(addressType.name()).inc();
                    break;
                case Released:
                    numReleased.labels(addressType.name()).inc();
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

        HTTPServer server = new HTTPServer(8080);

        Vertx vertx = Vertx.vertx();

        deployClients(vertx, endpointHost, endpointPort, AddressType.anycast, linksPerConnection, anycastAddresses);
        deployClients(vertx, endpointHost, endpointPort, AddressType.queue, linksPerConnection, queueAddresses);

        while (true) {
            Thread.sleep(30000);
            System.out.println("# Metrics");
            System.out.println("Successful connects = " + connectSuccesses.get());
            System.out.println("Failed connects = " + connectFailures.get());
            System.out.println("Disconnects = " + disconnects.get());
            System.out.println("Reconnects = " + reconnects.get());
            System.out.println("Reconnect duration (anycast) 99p = " + reconnectTime.get(AddressType.anycast).getValueAtPercentile(percentile));
            System.out.println("Reconnect duration (queue) 99p = " + reconnectTime.get(AddressType.queue).getValueAtPercentile(percentile));
            System.out.println("Reattach duration (anycast) 99p = " + reconnectTime.get(AddressType.anycast).getValueAtPercentile(percentile));
            System.out.println("Reattach duration (queue) 99p = " + reconnectTime.get(AddressType.queue).getValueAtPercentile(percentile));
            System.out.println("Num accepted = " + numAccepted.get());
            System.out.println("Num rejected = " + numRejected.get());
            System.out.println("Num modified = " + numModified.get());
            System.out.println("Num released = " + numReleased.get());
            System.out.println("##########");
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
