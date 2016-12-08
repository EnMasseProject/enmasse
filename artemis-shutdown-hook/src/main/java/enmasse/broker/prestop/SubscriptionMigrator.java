/*
 *  Copyright 2016 Red Hat Inc.
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
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Migrates messages from a single subscription to a destination host
 */
public class SubscriptionMigrator implements Callable<SubscriptionMigrator> {
    private final Vertx vertx;
    private final Host from;
    private final Host to;
    private final BrokerManager localBroker;
    private final String address;
    private final String queueName;
    private final String clientId;
    private final String subscriptionName;

    public SubscriptionMigrator(Vertx vertx, String address, String queueName, Host from, Host to, BrokerManager localBroker) {
        this.vertx = vertx;
        this.address = address;
        QueueName decomposedName = decomposeQueueNameForDurableSubscription(queueName);
        this.queueName = queueName;
        this.from = from;
        this.to = to;
        this.localBroker = localBroker;
        this.clientId = decomposedName.clientId;
        this.subscriptionName = decomposedName.subscriptionName;
    }

    private static class QueueName {
        String clientId;
        String subscriptionName;
        QueueName(String clientId, String subscriptionName) {
            this.clientId = clientId;
            this.subscriptionName = subscriptionName;
        }
    }

    // This code is stolen from Artemis internals so that we can decompose the queue name in the same way. This will not be necessary once we can use global link names.
    private static final char SEPARATOR = '.';
    private static QueueName decomposeQueueNameForDurableSubscription(final String queueName) {
        StringBuffer[] parts = new StringBuffer[2];
        int currentPart = 0;

        parts[0] = new StringBuffer();
        parts[1] = new StringBuffer();

        int pos = 0;
        while (pos < queueName.length()) {
            char ch = queueName.charAt(pos);
            pos++;

            if (ch == SEPARATOR) {
                currentPart++;
                if (currentPart >= parts.length) {
                    throw new RuntimeException("Invalid message queue name: " + queueName);
                }

                continue;
            }

            if (ch == '\\') {
                if (pos >= queueName.length()) {
                    throw new RuntimeException("Invalid message queue name: " + queueName);
                }
                ch = queueName.charAt(pos);
                pos++;
            }

            parts[currentPart].append(ch);
        }

        if (currentPart != 1) {
         /* JMS 2.0 introduced the ability to create "shared" subscriptions which do not require a clientID.
          * In this case the subscription name will be the same as the queue name, but the above algorithm will put that
          * in the wrong position in the array so we need to move it.
          */
            parts[1] = parts[0];
            parts[0] = new StringBuffer();
        }

        return new QueueName(parts[0].toString(), parts[1].toString());
    }


    public static boolean isValidSubscription(String queue) {
        try {
            decomposeQueueNameForDurableSubscription(queue);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private ProtonMessageHandler messageHandler(CountDownLatch latch, ProtonReceiver protonReceiver, ProtonSender protonSender) {
        return (sourceDelivery, message) -> protonSender.send(message, protonDelivery -> {
            message.setPriority(Short.MAX_VALUE);
            sourceDelivery.disposition(protonDelivery.getRemoteState(), protonDelivery.remotelySettled());
            latch.countDown();
            protonReceiver.flow(protonSender.getCredit() - protonReceiver.getCredit());
        });
    }

    private void createReceiver(CountDownLatch latch, ProtonSender sender) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        Endpoint endpoint = from.amqpEndpoint();
        protonClient.connect(endpoint.hostname(), endpoint.port(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer(clientId);
                conn.closeHandler(result -> {
                    System.out.println("Migrator sub connection closed");
                });
                conn.openHandler(result -> {
                    Source source = new Source();
                    source.setAddress(address);
                    source.setDurable(TerminusDurability.UNSETTLED_STATE);
                    source.setCapabilities(Symbol.getSymbol("topic"));
                    ProtonReceiver localReceiver = conn.createReceiver(subscriptionName, new ProtonLinkOptions().setLinkName(subscriptionName));
                    localReceiver.setSource(source);
                    localReceiver.setPrefetch(0);
                    localReceiver.setAutoAccept(false);
                    localReceiver.closeHandler(res -> System.out.println("Migrator sub receiver closed"));
                    localReceiver.openHandler(res -> {
                        if (res.succeeded()) {
                            System.out.println("Opened localReceiver for " + clientId + "." + subscriptionName);
                            localReceiver.flow(1);
                        } else {
                            System.out.println("Failed opening received: " + res.cause().getMessage());
                        }
                    });
                    localReceiver.handler(messageHandler(latch, localReceiver, sender));
                    localReceiver.open();
                });
                conn.open();
            } else {
                System.out.println("Connection failed: " + connection.cause().getMessage());
            }
        });
    }

    private void createSender(CountDownLatch latch) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        Endpoint endpoint = to.amqpEndpoint();
        protonClient.connect(endpoint.hostname(), endpoint.port(), toConnection -> {
            if (toConnection.succeeded()) {
                System.out.println("Opened connection to destination");
                ProtonConnection toConn = toConnection.result();
                toConn.setContainer("topic-migrator");
                toConn.closeHandler(result -> {
                    System.out.println("Migrator connection closed");
                });
                toConn.openHandler(toResult -> {
                    Target target = new Target();
                    target.setAddress(address);
                    target.setCapabilities(Symbol.getSymbol("topic"));
                    ProtonSender sender = toConn.createSender(subscriptionName);
                    sender.setTarget(target);
                    sender.closeHandler(res -> System.out.println("Sender connection closed"));
                    sender.openHandler(toRes -> {
                        if (toRes.succeeded()) {
                            System.out.println("Opened sender, marking ready!");
                            createReceiver(latch, sender);
                        } else {
                            System.out.println("Error opening sender: " + toRes.cause().getMessage());
                        }
                    });
                    sender.open();
                });
                toConn.open();
            } else {
                System.out.println("Failed opening connection: " + toConnection.cause().getMessage());
            }
        });
    }

    @Override
    public SubscriptionMigrator call() throws Exception {
        System.out.println("Calling migrator...");
        long numMessages = localBroker.getQueueMessageCount(queueName);
        CountDownLatch latch = new CountDownLatch(Math.toIntExact(numMessages));
        System.out.println("Migrating " + numMessages + " messages from " + queueName);
        createSender(latch);
        System.out.println("Waiting ....");
        latch.await();
        System.out.println("Done waiting!");
        return this;
    }
}
