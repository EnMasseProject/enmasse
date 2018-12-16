/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
                    log.info("Migrator connection for {} closed", queueInfo);
                });
                conn.openHandler(result -> {
                    Source source = new Source();
                    source.setAddress(queueInfo.getQualifiedAddress());
                    ProtonReceiver localReceiver = conn.createReceiver(queueInfo.getQualifiedAddress());
                    localReceiver.setSource(source);
                    localReceiver.setPrefetch(0);
                    localReceiver.setAutoAccept(false);
                    localReceiver.closeHandler(res -> log.info("Migrator receiver for {} closed", queueInfo));
                    localReceiver.openHandler(res -> {
                        if (res.succeeded()) {
                            log.info("Opened localReceiver for {}", queueInfo);
                            localReceiver.flow(1);
                        } else {
                            log.info("Failed opening receiver for {}: {}", queueInfo, res.cause().getMessage());
                        }
                    });
                    localReceiver.handler(messageHandler(latch, localReceiver, sender));
                    localReceiver.open();
                });
                conn.open();
            } else {
                log.info("Connection failed for {}: {}", queueInfo, connection.cause().getMessage());
            }
        });
    }

    private void createSender(CountDownLatch latch) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        Endpoint endpoint = to.amqpEndpoint();
        protonClient.connect(protonClientOptions, endpoint.hostname(), endpoint.port(), toConnection -> {
            if (toConnection.succeeded()) {
                log.info("Opened connection to destination broker for {}", queueInfo);
                ProtonConnection toConn = toConnection.result();
                toConn.setContainer("topic-migrator");
                toConn.closeHandler(result -> {
                    log.info("Migrator connection for {} closed", queueInfo);
                });
                toConn.openHandler(toResult -> {
                    Target target = new Target();
                    target.setAddress(queueInfo.getAddress());
                    ProtonSender sender = toConn.createSender(queueInfo.getAddress());
                    sender.setTarget(target);
                    sender.closeHandler(res -> log.info("Sender connection for {} closed", queueInfo));
                    sender.openHandler(toRes -> {
                        if (toRes.succeeded()) {
                            log.info("Opened sender for {}, marking ready!", queueInfo);
                            createReceiver(latch, sender);
                        } else {
                            log.info("Error opening sender for {}: {}", queueInfo, toRes.cause().getMessage());
                        }
                    });
                    sender.open();
                });
                toConn.open();
            } else {
                log.info("Failed opening sender connection for {}: {}", queueInfo, toConnection.cause().getMessage());
            }
        });
    }

    @Override
    public QueueMigrator call() throws Exception {
        log.info("Calling migrator...");
        long numMessages = broker.getQueueMessageCount(queueInfo.getQueueName());
        CountDownLatch latch = new CountDownLatch(Math.toIntExact(numMessages));
        log.info("Migrating " + numMessages + " messages for queue " + queueInfo);
        createSender(latch);
        log.info("Waiting ....");
        latch.await();
        log.info("Done waiting!");
        return this;
    }
}
