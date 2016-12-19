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

import com.google.common.io.Files;
import enmasse.discovery.Endpoint;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.RoutingType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;


public class TestBroker {
    private final String host;
    private final int port;
    private final String address;
    private final String messageAddress;
    private final EmbeddedActiveMQ server = new EmbeddedActiveMQ();
    private final Messenger messenger = Messenger.Factory.create();
    private final boolean multicast;

    public TestBroker(Endpoint endpoint, String address, boolean multicast) {
        this.host = endpoint.hostname();
        this.port = endpoint.port();
        this.address = address;
        this.messageAddress = String.format("amqp://%s:%s/%s", host, port, address);
        this.multicast = multicast;
    }

    public void start() throws Exception {
        Configuration config = new ConfigurationImpl();

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocols", "AMQP,CORE");
        params.put("host", host);
        params.put("port", port);
        TransportConfiguration transport = new TransportConfiguration(NettyAcceptorFactory.class.getName(), params, "amqp");

        CoreAddressConfiguration addressConfig = new CoreAddressConfiguration();
        addressConfig.setName(address);
        CoreQueueConfiguration queueConfig = new CoreQueueConfiguration();
        queueConfig.setAddress(address);
        queueConfig.setRoutingType(multicast ? RoutingType.MULTICAST : RoutingType.ANYCAST);
        queueConfig.setName(address);
        addressConfig.addQueueConfiguration(queueConfig);

        config.setAcceptorConfigurations(Collections.singleton(transport));
        config.setSecurityEnabled(false);
        config.setAddressConfigurations(Arrays.asList(addressConfig));
        config.setName("broker-" + System.currentTimeMillis() + port);
        config.setBindingsDirectory(Files.createTempDir().getAbsolutePath());
        config.setJournalDirectory(Files.createTempDir().getAbsolutePath());
        config.setPagingDirectory(Files.createTempDir().getAbsolutePath());
        config.setLargeMessagesDirectory(Files.createTempDir().getAbsolutePath());
        config.setPersistenceEnabled(false);

        server.setConfiguration(config);

        server.start();
        Thread.sleep(2000);
        messenger.start();
    }

    public void sendMessage(String messageBody) throws IOException {
        Message message = Message.Factory.create();
        message.setAddress(messageAddress);
        message.setBody(new AmqpValue(messageBody));

        messenger.put(message);
        messenger.send();
    }

    public String recvMessage() throws IOException {
        messenger.subscribe(messageAddress);
        messenger.recv(1);
        Message message = messenger.get();
        return (String) ((AmqpValue)message.getBody()).getValue();
    }

    public int numConnected() {
        return server.getActiveMQServer().getConnectionCount();
    }

    public void stop() throws Exception {
        messenger.stop();
        server.stop();
    }

    public boolean isActive() {
        return server.getActiveMQServer().isActive();
    }

    public void assertShutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (isActive() && System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
        }
        assertFalse("Server has not been shut down", isActive());
    }
}
