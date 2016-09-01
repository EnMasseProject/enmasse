package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryListener;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ulf Lilleengen
 */
public class ForwarderController implements DiscoveryListener {
    private static final Logger log = LoggerFactory.getLogger(ForwarderController.class.getName());

    private final Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
    private final Map<Host, Forwarder> replicatedHosts = new HashMap<>();

    private final Host localHost;
    private final String address;
    private final long connectionRetryInterval = 5000;

    public ForwarderController(Host localHost, String address) {
        this.localHost = localHost;
        this.address = address;
    }

    @Override
    public synchronized void hostsChanged(Set<Host> hosts) {
        hosts.remove(localHost);

        log.debug("Hosts changed to " + hosts);
        createForwarders(hosts);
        deleteForwarders(hosts);
    }

    private void createForwarders(Set<Host> newHosts) {
        Set<Host> currentHosts = replicatedHosts.keySet();
        Set<Host> hostsToCreate = new HashSet<>(newHosts);
        hostsToCreate.removeAll(currentHosts);
        hostsToCreate.forEach(this::createForwarder);
    }

    private void deleteForwarders(Set<Host> newHosts) {
        Set<Host> hostsToRemove = new HashSet<>(replicatedHosts.keySet());
        hostsToRemove.removeAll(newHosts);
        hostsToRemove.forEach(this::deleteForwarder);
    }

    private void deleteForwarder(Host host) {
        Forwarder forwarder = replicatedHosts.remove(host);
        log.info("Deleting forwarder " + forwarder);
        assert (forwarder != null);
        vertx.runOnContext(event -> forwarder.stop());
    }

    private void createForwarder(Host host) {
        Forwarder forwarder = new Forwarder(vertx, localHost, host, address, connectionRetryInterval);
        log.info("Creating forwarder " + forwarder);
        replicatedHosts.put(host, forwarder);
        forwarder.start();
    }
}
