/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import com.google.common.io.Files;
import io.enmasse.amqp.BlockingClient;
import enmasse.discovery.Endpoint;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;


public class TestBroker {
    private final String host;
    private final int port;
    private final Set<String> addressList = new HashSet<>();
    private final EmbeddedActiveMQ server = new EmbeddedActiveMQ();
    private final BlockingClient client;
    private final boolean multicast;

    public TestBroker(Endpoint endpoint, Collection<String> addressList, boolean multicast) {
        this.host = endpoint.hostname();
        this.port = endpoint.port();
        this.addressList.addAll(addressList);
        this.multicast = multicast;
        this.client = new BlockingClient(endpoint.hostname(), endpoint.port());
    }

    public void start() throws Exception {
        Configuration config = new ConfigurationImpl();

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocols", "AMQP,CORE");
        params.put("host", host);
        params.put("port", port);
        TransportConfiguration transport = new TransportConfiguration(NettyAcceptorFactory.class.getName(), params, "amqp");

        for (String address : addressList) {
            CoreAddressConfiguration addressConfig = new CoreAddressConfiguration();
            addressConfig.setName(address);
            CoreQueueConfiguration queueConfig = new CoreQueueConfiguration();
            queueConfig.setAddress(address);
            queueConfig.setRoutingType(multicast ? RoutingType.MULTICAST : RoutingType.ANYCAST);
            queueConfig.setName(address);
            addressConfig.addQueueConfiguration(queueConfig);
            config.addAddressConfiguration(addressConfig);
        }

        config.setAcceptorConfigurations(Collections.singleton(transport));
        config.setSecurityEnabled(false);
        config.setName("broker-" + System.currentTimeMillis() + port);
        config.setBindingsDirectory(Files.createTempDir().getAbsolutePath());
        config.setJournalDirectory(Files.createTempDir().getAbsolutePath());
        config.setPagingDirectory(Files.createTempDir().getAbsolutePath());
        config.setLargeMessagesDirectory(Files.createTempDir().getAbsolutePath());
        config.setPersistenceEnabled(false);

        server.setConfiguration(config);

        server.start();
        Thread.sleep(2000);
    }

    public int numConnected() {
        return server.getActiveMQServer().getConnectionCount();
    }

    public void stop() throws Exception {
        client.close();
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

    public void sendMessages(String address, List<String> messages) throws InterruptedException {
        client.send(address, messages.stream()
                .map(body -> {
                    Message message = Message.Factory.create();
                    message.setAddress(address);
                    message.setBody(new AmqpValue(body));
                    return message;
                })
                .collect(Collectors.toList()),
                1, TimeUnit.MINUTES);
    }

    public List<String> recvMessages(String address, int numMessages) throws IOException, InterruptedException {
        List<Message> messages = client.recv(address, numMessages, 1, TimeUnit.MINUTES);
        return messages.stream()
                .map(m -> (String)((AmqpValue)m.getBody()).getValue())
                .collect(Collectors.toList());
    }

    public long numMessages(String queueName) throws Exception {
        return server.getActiveMQServer().getPostOffice().listQueuesForAddress(new SimpleString(queueName)).get(0).getMessageCount();
    }
}
