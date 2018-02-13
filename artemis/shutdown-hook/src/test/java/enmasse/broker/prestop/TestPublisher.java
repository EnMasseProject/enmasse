/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestPublisher implements AutoCloseable {
    private final Vertx vertx = Vertx.vertx();

    public void publish(Endpoint endpoint, String address, String content) throws InterruptedException {
        String containerId = "test-publisher";
        ProtonClient client = ProtonClient.create(vertx);
        CountDownLatch latch = new CountDownLatch(1);
        client.connect(endpoint.hostname(), endpoint.port(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer(containerId);
                conn.openHandler(result -> {
                    System.out.println("Connected: " + result.result().getRemoteContainer());
                    Target target = new Target();
                    target.setAddress(address);
                    target.setCapabilities(Symbol.getSymbol("topic"));
                    target.setDurable(TerminusDurability.UNSETTLED_STATE);
                    ProtonSender sender = conn.createSender(address);
                    sender.setTarget(target);
                    sender.openHandler(res -> {
                        if (res.succeeded()) {
                            System.out.println("Opened sender");
                            Message message = Message.Factory.create();
                            message.setAddress(address);
                            message.setBody(new AmqpValue(content));
                            sender.send(message, protonDelivery -> latch.countDown());
                        } else {
                            System.out.println("Failed opening sender: " + res.cause().getMessage());
                        }
                    });
                    sender.open();
                });
                conn.open();
            } else {
                System.out.println("Connection failed: " + connection.cause().getMessage());
            }
        });
        latch.await(1, TimeUnit.MINUTES);
    }

    @Override
    public void close() throws Exception {
        vertx.close();
    }
}
