package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryListener;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
    private final Logger log = Logger.getLogger(BrokerReplicator.class.getName());

    public BrokerReplicator(Host localHost, String address) {
        this.localHost = localHost;
        this.address = address;
    }

    @Override
    public synchronized void hostsChanged(Set<Host> hosts) {
        hosts.remove(lookupSelf(hosts));

        log.log(Level.DEBUG, "Connecting to " + hosts.size() + " hosts");
        createSenders(hosts);
        deleteSenders(hosts);
    }

    private Host lookupSelf(Set<Host> hosts) {
        for (Host host : hosts) {
            if (host.getHostname().equals(localHost.getHostname())) {
                return host;
            }
        }
        return localHost;
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
        log.log(Level.DEBUG, "Closing connection to " + host.getPortMap().get("amqp"));
        assert(connection != null);
        vertx.runOnContext(event -> connection.close());
    }


    private void connectToHost(Host host) {
        log.log(Level.DEBUG, "Creating sender to " + host.getPortMap().get("amqp"));
        client.connect(host.getHostname(), host.getPortMap().get("amqp"), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                ProtonSender sender = connection.createSender(address).open();
                log.log(Level.DEBUG, "Connected to broker, adding connection to " + host.getPortMap().get("amqp"));
                connectedHosts.put(host, new BrokerConnection(connection, sender.getSession(), sender));
            } else {
                log.log(Level.DEBUG, "Connection to broker " + host.getPortMap().get("amqp") + " failed");
            }
        });
    }

    public void start() {
        log.log(Level.INFO, "Connecting client to local broker " + localHost.getHostname());
        client.connect(localHost.getHostname(), localHost.getPortMap().get("amqp"), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                connection.createReceiver(address).handler((delivery, message) -> {
                    if (message.getAddress().equals(address) && !BrokerConnection.isMessageReplicated(message)) {
                        log.log(Level.DEBUG, "Forwarding message to brokers");
                        connectedHosts.values().forEach(brokerConnection -> brokerConnection.forwardMessage(message, address));
                    }
                }).open();
            }
        });
    }
}
