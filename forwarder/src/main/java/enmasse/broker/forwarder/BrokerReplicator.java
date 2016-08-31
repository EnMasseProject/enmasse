package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryListener;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ulf Lilleengen
 */
public class BrokerReplicator implements DiscoveryListener {
    private final Vertx vertx = Vertx.vertx();
    private final ProtonClient client = ProtonClient.create(vertx);
    private final Map<Host, ReplicatedBroker> replicatedHosts = new ConcurrentHashMap<>();
    private final Host localHost;
    private final String address;
    private final Logger log = LoggerFactory.getLogger(BrokerReplicator.class.getName());
    private final long connectionRetryInterval = 5000;

    // This is used as a placeholder to ensure connections are not duplicated in the connection phase
    private static final ReplicatedBroker dummyConnection = new ReplicatedBroker() {
        @Override
        public void forwardMessage(Message message, String forwardAddress) {

        }

        @Override
        public void close() {

        }
    };

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
        Set<Host> currentHosts = replicatedHosts.keySet();
        Set<Host> hostsToCreate = new HashSet<>(newHosts);
        hostsToCreate.removeAll(currentHosts);
        log.info("Creating hosts " + hostsToCreate);
        hostsToCreate.forEach(this::connectToHost);
    }

    private void deleteSenders(Set<Host> newHosts) {
        Set<Host> hostsToRemove = new HashSet<>(replicatedHosts.keySet());
        hostsToRemove.removeAll(newHosts);
        log.info("Deleting hosts " + hostsToRemove);
        hostsToRemove.forEach(this::closeSender);
    }

    private void closeSender(Host host) {
        ReplicatedBroker connection = replicatedHosts.remove(host);
        log.debug("Closing connection to " + host.getPortMap().get("amqp"));
        assert(connection != null);
        vertx.runOnContext(event -> connection.close());
    }


    private void connectToHost(Host host) {
        log.info("Creating sender to " + host.getHostname() + ":" + host.getPortMap().get("amqp"));
        replicatedHosts.put(host, dummyConnection);

        client.connect(host.getHostname(), host.getPortMap().get("amqp"), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                ProtonSender sender = connection.createSender(address).open();
                Target target = new Target();
                target.setAddress(address);
                target.setCapabilities(Symbol.getSymbol("topic"));
                sender.setTarget(target);
                log.info("Connected to broker, adding connection to " + host.getHostname() + ":" + host.getPortMap().get("amqp"));
                replicatedHosts.put(host, new BrokerConnection(connection, sender.getSession(), sender));
            } else {
                log.info("Connection to broker " + host.getHostname() + ":" + host.getPortMap().get("amqp") + " failed");
                vertx.setTimer(connectionRetryInterval, timerId -> connectToHost(host));
            }
        });
    }

    public void start() {
        log.info("Connecting client to local broker " + localHost.getHostname());
        client.connect(localHost.getHostname(), localHost.getPortMap().get("amqp"), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result().open();
                Source source = new Source();
                source.setAddress(address);
                source.setCapabilities(Symbol.getSymbol("topic"));

                connection.createReceiver(address)
                        .closeHandler(result -> {
                            if (result.succeeded()) {
                                log.info("Receiver closed");
                            } else {
                                log.warn("Receiver closed with error: " + result.cause().getMessage());
                                connection.close();
                                vertx.setTimer(connectionRetryInterval, timerId -> start());
                            }
                        })
                        .setSource(source)
                        .handler((delivery, message) -> {
                            if (log.isDebugEnabled()) {
                                log.debug("Received message from broker, forwarding");
                            }
                            if (message.getAddress().equals(address) && !BrokerConnection.isMessageReplicated(message)) {
                                replicatedHosts.values().forEach(brokerConnection -> brokerConnection.forwardMessage(message, address));
                            }
                        }).open();
            } else {
                log.warn("Error connecting to self, retrying");
                vertx.setTimer(connectionRetryInterval, timerId -> start());
            }
        });
    }
}
