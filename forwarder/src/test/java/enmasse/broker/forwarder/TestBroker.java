package enmasse.broker.forwarder;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author Ulf Lilleengen
 */
public class TestBroker {
    private final String host;
    private final int port;
    private final String address;
    private final EmbeddedActiveMQ server = new EmbeddedActiveMQ();

    public TestBroker(String host, int port, String address) {
        this.host = host;
        this.port = port;
        this.address = address;
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
        config.setName("broker-" + port);

        server.setConfiguration(config);

        server.start();
    }

    public int numConnected() {
        return server.getActiveMQServer().getConnectionCount();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
