/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import io.enmasse.amqp.Artemis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Adapts Artemis to the Broker interface used by the queue scheduler
 */
public class ArtemisAdapter implements Broker {
    private static final Logger log = LoggerFactory.getLogger(ArtemisAdapter.class);
    private final Artemis artemis;
    private static final String messagingHost = System.getenv("MESSAGING_SERVICE_HOST");
    private static final String messagingPort = System.getenv("MESSAGING_SERVICE_PORT_AMQPS_BROKER");

    public ArtemisAdapter(Artemis artemis) {
        this.artemis = artemis;
    }

    @Override
    public Set<String> getQueueNames() throws TimeoutException {
        Set<String> queues = artemis.getQueueNames();
        Set<String> connectors = artemis.getConnectorNames();

        queues.retainAll(connectors);
        return queues;
    }

    @Override
    public void createQueue(String address) throws TimeoutException {
        artemis.createQueue(address, address);
        Map<String, String> connectorParams = new HashMap<>();
        connectorParams.put("host", messagingHost);
        connectorParams.put("port", messagingPort);
        connectorParams.put("containerId", address);
        connectorParams.put("clusterId", address);
        artemis.createConnectorService(address, connectorParams);
    }

    @Override
    public void deleteQueue(String address) throws TimeoutException {
        artemis.destroyConnectorService(address);
        artemis.destroyQueue(address);
    }
}
