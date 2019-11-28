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
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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

    private static final Counter successCounter = Counter.build()
            .name("test_probe_success_total")
            .help("N/A")
            .register();

    private static final Counter failureCounter = Counter.build()
            .name("test_probe_failure_total")
            .help("N/A")
            .register();

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
                .withDisableHostnameVerification(true)
                .withTrustCerts(true)
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

        for (int i = 0; i < addresses.size(); i++) {
            String address = addresses.get(i);
            String name = String.format("%s.%s", addressSpace, address);
            final Address resource = new AddressBuilder()
                    .editOrNewMetadata()
                    .withName(name)
                    .addToLabels("client", "probe-client")
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
        }

        HTTPServer httpServer = new HTTPServer(8080);

        Vertx vertx = Vertx.vertx();
        while (true) {
            CountDownLatch completed = new CountDownLatch(addresses.size());
            for (String address : addresses) {
                vertx.deployVerticle(new ProbeClient(endpointHost, endpointPort, address), result -> {
                    if (result.succeeded()) {
                        successCounter.inc();
                    } else {
                        failureCounter.inc();
                    }
                    completed.countDown();
                });
            }
            completed.await();
            Thread.sleep(10000);
            System.out.println("# Metrics");
            System.out.println("successCounter = " + successCounter.get());
            System.out.println("failureCounter = " + failureCounter.get());
            System.out.println("##########");
        }
    }
}
