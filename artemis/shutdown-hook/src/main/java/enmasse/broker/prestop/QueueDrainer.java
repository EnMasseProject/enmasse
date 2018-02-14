/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.enmasse.amqp.Artemis;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client for draining messages from an endpoint and forward to a target endpoint, until empty.
 */
public class QueueDrainer {
    private final Logger log = LoggerFactory.getLogger(QueueDrainer.class);
    private final Vertx vertx;
    private final Host fromHost;
    private final BrokerFactory brokerFactory;
    private final Optional<Runnable> debugFn;
    private final ProtonClientOptions protonClientOptions;

    public QueueDrainer(Vertx vertx, Host from, BrokerFactory brokerFactory, ProtonClientOptions clientOptions, Optional<Runnable> debugFn) throws Exception {
        this.vertx = vertx;
        this.fromHost = from;
        this.brokerFactory = brokerFactory;
        this.protonClientOptions = clientOptions;
        this.debugFn = debugFn;
    }

    private Set<String> getQueues(Artemis broker) throws Exception {
        Set<String> addresses = new HashSet<>(broker.getQueueNames());
        addresses.removeIf(a -> a.startsWith("activemq.management"));
        return addresses;
    }

    public void drainMessages(Endpoint to, String queueName) throws Exception {
        Artemis broker = brokerFactory.createClient(vertx, protonClientOptions, fromHost.amqpEndpoint());

        if (queueName != null && !queueName.isEmpty()) {
            broker.destroyConnectorService("amqp-connector");
            startDrain(to, queueName);
            log.info("Waiting.....");
            waitUntilEmpty(broker, Collections.singleton(queueName));
        } else {
            Set<String> addresses = getQueues(broker);

            for (String address : addresses) {
                broker.destroyConnectorService(address);
                startDrain(to, address);
            }
            log.info("Waiting.....");
            waitUntilEmpty(broker, addresses);
        }
        log.info("Done waiting!");
        broker.forceShutdown();;
        vertx.close();
    }

    private void startDrain(Endpoint to, String address) {
        AtomicBoolean first = new AtomicBoolean(false);
        Endpoint from = fromHost.amqpEndpoint();
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(protonClientOptions, to.hostname(), to.port(), sendHandle -> {
            if (sendHandle.succeeded()) {
                ProtonConnection sendConn = sendHandle.result();
                sendConn.setContainer("shutdown-hook-sender");
                sendConn.openHandler(ev -> {
                    log.info("Connected to sender: " +  sendConn.getRemoteContainer());
                });
                sendConn.open();
                ProtonSender sender = sendConn.createSender(address);
                sender.openHandler(handle -> {
                    if (handle.succeeded()) {
                        client.connect(protonClientOptions, from.hostname(), from.port(), recvHandle -> {
                            if (recvHandle.succeeded()) {
                                ProtonConnection recvConn = recvHandle.result();
                                recvConn.setContainer("shutdown-hook-recv");
                                recvConn.closeHandler(h -> {
                                    log.info("Receiver closed: " + h.succeeded());
                                });
                                log.info("Connected to receiver: " + recvConn.getRemoteContainer());
                                recvConn.openHandler(h -> {
                                    log.info("Receiver other end opened: " + h.result().getRemoteContainer());
                                    markInstanceDeleted(recvConn.getRemoteContainer());
                                    ProtonReceiver receiver = recvConn.createReceiver(address);
                                    receiver.setPrefetch(0);
                                    receiver.openHandler(handler -> {
                                        log.info("Receiver open: " + handle.succeeded());
                                        receiver.flow(1);
                                    });
                                    receiver.handler((protonDelivery, message) -> {
                                        //System.out.println("Got Message to forwarder: " + ((AmqpValue)message.getBody()).getValue());
                                        sender.send(message, targetDelivery -> {
                                                log.info("Got delivery confirmation, id = " + message.getMessageId() + ", remoteState = " + targetDelivery.getRemoteState() + ", remoteSettle = " + targetDelivery.remotelySettled());
                                                receiver.flow(1);
                                                protonDelivery.disposition(targetDelivery.getRemoteState(), targetDelivery.remotelySettled()); });

                                        // This is for debugging only
                                        if (!first.getAndSet(true)) {
                                            log.info("Forwarded first message");
                                            if (debugFn.isPresent()) {
                                                vertx.executeBlocking((Future<Integer> future) -> {
                                                    debugFn.get().run();
                                                    future.complete(0);
                                                }, (AsyncResult<Integer> result) -> {
                                                });
                                            }
                                        }
                                    });
                                    receiver.open();
                                });
                                recvConn.open();
                            } else {
                                log.warn("Error connecting to receiver " + from.hostname() + ":" + from.port() + ": " + recvHandle.cause().getMessage());
                                sendConn.close();
                                vertx.setTimer(5000, id -> startDrain(to, address));
                            }
                        });
                    } else {
                        log.warn("Failed to open sender: " + handle.cause().getMessage());
                        vertx.setTimer(5000, id -> startDrain(to, address));
                    }
                });
                sender.open();
            } else {
                log.warn("Error connecting to sender " + to.hostname() + ":" + to.port() + ": " + sendHandle.cause().getMessage()); ;
                vertx.setTimer(5000, id -> startDrain(to, address));
            }
        });
    }

    private void markInstanceDeleted(String instanceName) {
        try {
            File instancePath = new File("/var/run/artemis", instanceName);
            if (instancePath.exists()) {
                Files.write(new File(instanceName, "terminating").toPath(), "yes".getBytes(), StandardOpenOption.WRITE);
                log.info("Instance " + instanceName + " marked as terminating");
            }
        } catch (IOException e) {
            log.warn("Error deleting instance: " + e.getMessage());
        }
    }

    private void waitUntilEmpty(Artemis broker, Collection<String> queues) throws InterruptedException {
        while (true) {
            try {
                long count = 0;
                for (String queue : queues) {
                    count += broker.getQueueMessageCount(queue);
                    log.info("Found " + count + " messages in queue " + queue);
                }
                if (count == 0) {
                    break;
                }
            } catch (Exception e) {
                // Retry
                log.warn("Queue check failed: " + e.getMessage());
            }
            Thread.sleep(2000);
        }
    }

}
