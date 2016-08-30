package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryListener;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ulf Lilleengen
 */
public class BrokerReplicator implements DiscoveryListener {
    private final Vertx vertx = Vertx.vertx();
    private final ProtonClient client = ProtonClient.create(vertx);
    private final Map<Host, BrokerConnection> connectedHosts = new ConcurrentHashMap<>();
    private final Host localHost;
    private final String address;
    private final Logger log = LoggerFactory.getLogger(BrokerReplicator.class.getName());

    public BrokerReplicator(Host localHost, String address) {
        this.localHost = localHost;
        this.address = address;
    }

    @Override
    public synchronized void hostsChanged(Set<Host> hosts) {
        hosts.remove(localHost);

        log.debug("Connecting to " + hosts.size() + " hosts");
        createSenders(hosts);
        deleteSenders(hosts);
    }

    private void createSenders(Set<Host> newHosts) {
        Set<Host> currentHosts = connectedHosts.keySet();
        newHosts.removeAll(currentHosts);
        newHosts.forEach(this::connectToHost);
    }

    private void deleteSenders(Set<Host> newHosts) {
        Set<Host> currentHosts = connectedHosts.keySet();
        currentHosts.removeAll(newHosts);
        currentHosts.forEach(this::closeSender);
    }

    private void closeSender(Host host) {
        BrokerConnection connection = connectedHosts.remove(host);
        log.debug("Closing connection to " + host.getPortMap().get("amqp"));
        assert(connection != null);
        vertx.runOnContext(event -> connection.close());
    }


    private void connectToHost(Host host) {
        log.info("Creating sender to " + host.getPortMap().get("amqp"));
        client.connect(host.getHostname(), host.getPortMap().get("amqp"), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                ProtonSender sender = connection.createSender(address).open();
                log.info("Connected to broker, adding connection to " + host.getPortMap().get("amqp"));
                connectedHosts.put(host, new BrokerConnection(connection, sender.getSession(), sender));
            } else {
                log.debug("Connection to broker " + host.getPortMap().get("amqp") + " failed");
            }
        });
    }

    public void start() {
        log.info("Connecting client to local broker " + localHost.getHostname());
        client.connect(localHost.getHostname(), localHost.getPortMap().get("amqp"), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                connection.createReceiver(address).handler((delivery, message) -> {
                    if (message.getAddress().equals(address) && !BrokerConnection.isMessageReplicated(message)) {
                        log.debug("Forwarding message to brokers");
                        connectedHosts.values().forEach(brokerConnection -> brokerConnection.forwardMessage(message, address));
                    }
                }).open();
            }
        });
    }
}
