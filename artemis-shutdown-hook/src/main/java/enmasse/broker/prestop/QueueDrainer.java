/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client for draining messages from an endpoint and forward to a target endpoint, until empty.
 */
public class QueueDrainer {

    private final Vertx vertx = Vertx.vertx();
    private final ProtonClient client = ProtonClient.create(vertx);
    private final Host fromHost;
    private final Optional<Runnable> debugFn;

    public QueueDrainer(Host from, Optional<Runnable> debugFn) throws Exception {
        this.fromHost = from;
        this.debugFn = debugFn;
    }

    public void drainMessages(Endpoint to, String address) throws Exception {
        BrokerManager brokerManager = new BrokerManager(fromHost.coreEndpoint());

        brokerManager.destroyConnectorService("amqp-connector");
        startDrain(to, address);
        System.out.println("Waiting.....");
        brokerManager.waitUntilEmpty(address);
        System.out.println("Done waiting!");
        vertx.close();
        brokerManager.shutdownBroker();
    }

    public void startDrain(Endpoint to, String address) {
        AtomicBoolean first = new AtomicBoolean(false);
        Endpoint from = fromHost.amqpEndpoint();
        client.connect(to.hostname(), to.port(), sendHandle -> {
            if (sendHandle.succeeded()) {
                ProtonConnection sendConn = sendHandle.result();
                sendConn.setContainer("shutdown-hook-sender");
                sendConn.openHandler(ev -> {
                    System.out.println("Connected to sender: " +  sendConn.getRemoteContainer());
                });
                sendConn.open();
                ProtonSender sender = sendConn.createSender(address);
                sender.openHandler(handle -> {
                    if (handle.succeeded()) {
                        client.connect(from.hostname(), from.port(), recvHandle -> {
                            if (recvHandle.succeeded()) {
                                ProtonConnection recvConn = recvHandle.result();
                                recvConn.setContainer("shutdown-hook-recv");
                                recvConn.closeHandler(h -> {
                                    System.out.println("Receiver closed: " + h.succeeded());
                                });
                                System.out.println("Connected to receiver: " + recvConn.getRemoteContainer());
                                recvConn.openHandler(h -> {
                                    System.out.println("Receiver other end opened: " + h.result().getRemoteContainer());
                                    markInstanceDeleted(recvConn.getRemoteContainer());
                                    ProtonReceiver receiver = recvConn.createReceiver(address);
                                    receiver.setPrefetch(0);
                                    receiver.openHandler(handler -> {
                                        System.out.println("Receiver open: " + handle.succeeded());
                                        receiver.flow(100);
                                    });
                                    receiver.handler((protonDelivery, message) -> {
                                        System.out.println("Got Message to forwarder");
                                        sender.send(message, targetDelivery ->
                                                protonDelivery.disposition(targetDelivery.getRemoteState(), targetDelivery.remotelySettled()));

                                        // This is for debugging only
                                        if (!first.getAndSet(true)) {
                                            System.out.println("Forwarded one message");
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
                                System.out.println("Error connecting to receiver " + from.hostname() + ":" + from.port() + ": " + recvHandle.cause().getMessage());
                                sendConn.close();
                                vertx.setTimer(5000, id -> startDrain(to, address));
                            }
                        });
                    } else {
                        System.out.println("Failed to open sender: " + handle.cause().getMessage());
                        vertx.setTimer(5000, id -> startDrain(to, address));
                    }
                });
                sender.open();
            } else {
                System.out.println("Error connecting to sender " + to.hostname() + ":" + to.port() + ": " + sendHandle.cause().getMessage()) ;
                vertx.setTimer(5000, id -> startDrain(to, address));
            }
        });
    }

    private void markInstanceDeleted(String instanceName) {
        try {
            File instancePath = new File("/var/run/artemis", instanceName);
            if (instancePath.exists()) {
                Files.createFile(new File(instancePath, "enmasse-deleted").toPath());
                System.out.println("Instance " + instanceName + " marked as deleted");
            }
        } catch (IOException e) {
            System.out.println("Error deleting instance: " + e.getMessage());
        }
    }
}
