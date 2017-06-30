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

package enmasse.broker.forwarder;

import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestBroker {
    private final String host;
    private final int port;
    private final String address;
    private final EmbeddedActiveMQ server = new EmbeddedActiveMQ();
    private final ProtonClient protonClient;

    public TestBroker(ProtonClient client, String host, int port, String address) {
        this.protonClient = client;
        this.host = host;
        this.port = port;
        this.address = address;
    }

    public void start() throws Exception {
        Configuration config = new ConfigurationImpl();
        config.setPersistenceEnabled(false);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocols", "AMQP");
        params.put("host", host);
        params.put("port", port);
        TransportConfiguration transport = new TransportConfiguration(NettyAcceptorFactory.class.getName(), params, "amqp");

        config.setAcceptorConfigurations(Collections.singleton(transport));
        config.setSecurityEnabled(false);
        config.setName("broker-" + port);


        CoreAddressConfiguration addressConfig = new CoreAddressConfiguration();
        addressConfig.setName(address);
        addressConfig.addRoutingType(RoutingType.MULTICAST);
        config.addAddressConfiguration(addressConfig);
        server.setConfiguration(config);

        server.start();
    }

    public void sendMessage(String messageBody, long timeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        protonClient.connect(host, port, event -> {
            ProtonConnection connection = event.result().open();
            Target target = new Target();
            target.setAddress(address);
            target.setCapabilities(Symbol.getSymbol("topic"));
            ProtonSender sender = connection.createSender(address);
            sender.setTarget(target);
            sender.open();
            Message message = Message.Factory.create();
            message.setBody(new AmqpValue(messageBody));
            message.setAddress(address);
            sender.send(message, delivery -> latch.countDown());
        });
        latch.await(timeout, timeUnit);
    }

    public CompletableFuture<List<String>> recvMessages(long numMessages, long attachTimeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        List<String> messages = new ArrayList<>();
        protonClient.connect(host, port, event -> {
            ProtonConnection connection = event.result().open();
            Source source = new Source();
            source.setAddress(address);
            source.setCapabilities(Symbol.getSymbol("topic"));
            connection.createReceiver(address)
                    .openHandler(opened -> latch.countDown())
                    .setSource(source)
                    .handler((delivery, message) -> {
                        messages.add((String) ((AmqpValue) message.getBody()).getValue());
                        if (messages.size() == numMessages) {
                            future.complete(new ArrayList<>(messages));
                        }
                    })
                    .open();
        });
        latch.await(attachTimeout, timeUnit);
        return future;
    }

    public int numConnected() {
        return server.getActiveMQServer().getConnectionCount();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
