/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import com.google.common.io.Files;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class TestArtemis {
    private final String host;
    private final int port;
    private final EmbeddedActiveMQ server = new EmbeddedActiveMQ();

    public TestArtemis(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        Configuration config = new ConfigurationImpl();

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocols", "AMQP");
        params.put("host", host);
        params.put("port", port);
        TransportConfiguration transport = new TransportConfiguration(NettyAcceptorFactory.class.getName(), params, "amqp");

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
        server.stop();
    }

    public boolean isActive() {
        return server.getActiveMQServer().isActive();
    }

    public ActiveMQServer getServer() {
        return server.getActiveMQServer();
    }
}
