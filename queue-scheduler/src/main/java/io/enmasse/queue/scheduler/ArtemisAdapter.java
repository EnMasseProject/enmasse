/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.queue.scheduler;

import io.enmasse.amqp.Artemis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Adapts Artemis to the Broker interface used by the queue scheduler
 */
public class ArtemisAdapter implements Broker {
    private final Artemis artemis;
    private static final String messagingHost = System.getenv("MESSAGING_SERVICE_HOST");
    private static final String messagingPort = System.getenv("MESSAGING_SERVICE_PORT_AMQPS_BROKER");

    public ArtemisAdapter(Artemis artemis) {
        this.artemis = artemis;
    }

    @Override
    public Set<String> getQueueNames() throws TimeoutException {
        return artemis.getQueueNames();
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
