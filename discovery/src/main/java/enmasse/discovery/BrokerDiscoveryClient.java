package enmasse.discovery;

import java.util.Collections;
import java.util.Optional;

/**
 * A client for talking to the 'brokersense' discovery service a given and address, and notifying listeners of
 * the changing set of hosts.
 */
public class BrokerDiscoveryClient extends BaseDiscoveryClient {
    public BrokerDiscoveryClient(Endpoint endpoint, String group, Optional<String> containerName) {
        super(endpoint, "brokersense", Collections.singletonMap("group_id", group), containerName);
    }
}
