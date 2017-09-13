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
import io.enmasse.amqp.Artemis;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Migrates messages from a single subscription to a destination host
 */
public class QueueMigrator implements Callable<QueueMigrator> {
    private final Logger log = LoggerFactory.getLogger(QueueMigrator.class);
    private final Vertx vertx;
    private final Host from;
    private final Host to;
    private final Artemis broker;
    private final QueueInfo queueInfo;
    private final ProtonClientOptions protonClientOptions;

    public QueueMigrator(Vertx vertx, QueueInfo queueInfo, Host from, Host to, Artemis broker, ProtonClientOptions protonClientOptions) {
        this.protonClientOptions = protonClientOptions;
        this.vertx = vertx;
        this.queueInfo = queueInfo;
        this.from = from;
        this.to = to;
        this.broker = broker;
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
        protonClient.connect(protonClientOptions, endpoint.hostname(), endpoint.port(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.closeHandler(result -> {
                    log.info("Migrator sub connection closed");
                });
                conn.openHandler(result -> {
                    Source source = new Source();
                    source.setAddress(queueInfo.getQualifiedAddress());
                    ProtonReceiver localReceiver = conn.createReceiver(queueInfo.getQualifiedAddress());
                    localReceiver.setSource(source);
                    localReceiver.setPrefetch(0);
                    localReceiver.setAutoAccept(false);
                    localReceiver.closeHandler(res -> log.info("Migrator sub receiver closed"));
                    localReceiver.openHandler(res -> {
                        if (res.succeeded()) {
                            log.info("Opened localReceiver for " + queueInfo);
                            localReceiver.flow(1);
                        } else {
                            log.info("Failed opening received: " + res.cause().getMessage());
                        }
                    });
                    localReceiver.handler(messageHandler(latch, localReceiver, sender));
                    localReceiver.open();
                });
                conn.open();
            } else {
                log.info("Connection failed: " + connection.cause().getMessage());
            }
        });
    }

    private void createSender(CountDownLatch latch) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        Endpoint endpoint = to.amqpEndpoint();
        protonClient.connect(protonClientOptions, endpoint.hostname(), endpoint.port(), toConnection -> {
            if (toConnection.succeeded()) {
                log.info("Opened connection to destination");
                ProtonConnection toConn = toConnection.result();
                toConn.setContainer("topic-migrator");
                toConn.closeHandler(result -> {
                    log.info("Migrator connection closed");
                });
                toConn.openHandler(toResult -> {
                    Target target = new Target();
                    target.setAddress(queueInfo.getAddress());
                    ProtonSender sender = toConn.createSender(queueInfo.getAddress());
                    sender.setTarget(target);
                    sender.closeHandler(res -> log.info("Sender connection closed"));
                    sender.openHandler(toRes -> {
                        if (toRes.succeeded()) {
                            log.info("Opened sender, marking ready!");
                            createReceiver(latch, sender);
                        } else {
                            log.info("Error opening sender: " + toRes.cause().getMessage());
                        }
                    });
                    sender.open();
                });
                toConn.open();
            } else {
                log.info("Failed opening connection: " + toConnection.cause().getMessage());
            }
        });
    }

    @Override
    public QueueMigrator call() throws Exception {
        log.info("Calling migrator...");
        long numMessages = broker.getQueueMessageCount(queueInfo.getQueueName());
        CountDownLatch latch = new CountDownLatch(Math.toIntExact(numMessages));
        log.info("Migrating " + numMessages + " messages from " + queueInfo);
        createSender(latch);
        log.info("Waiting ....");
        latch.await();
        log.info("Done waiting!");
        return this;
    }
}
