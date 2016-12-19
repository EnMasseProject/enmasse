/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.discovery;

import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A client for talking to the 'podsense' discovery service a given set of labels, and notifying listeners of
 * the changing set of hosts.
 */
public class DiscoveryClient extends AbstractVerticle {
    private final Map<Symbol, String> labelFilter;
    private final List<DiscoveryListener> listeners = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(DiscoveryClient.class.getName());
    private final Endpoint endpoint;

    public DiscoveryClient(Endpoint endpoint, Map<String, String> labelFilter) {
        this.endpoint = endpoint;
        this.labelFilter = toSymbolMap(labelFilter);
    }

    public DiscoveryClient(Map<String, String> labelFilter) {
        this(getPodSenseEndpoint(), labelFilter);
    }

    private static Endpoint getPodSenseEndpoint() {
        String host = System.getenv("ADMIN_SERVICE_HOST");
        String port = System.getenv("ADMIN_SERVICE_PORT_CONFIGURATION");
        if (host == null) {
            host = System.getenv("CONFIGURATION_SERVICE_HOST");
            port = System.getenv("CONFIGURATION_SERVICE_PORT");
        }
        return new Endpoint(host, Integer.parseInt(port));
    }

    private static Map<Symbol, String> toSymbolMap(Map<String, String> labelFilter) {
        Map<Symbol, String> symbolMap = new HashMap<>();
        for (Map.Entry<String, String> entry : labelFilter.entrySet()) {
            symbolMap.put(Symbol.getSymbol(entry.getKey()), entry.getValue());
        }
        return symbolMap;
    }

    public void addListener(DiscoveryListener listener) {
        this.listeners.add(listener);
    }

    private void notifyListeners(Set<Host> hosts) {
        log.debug("Received new set of hosts: " + hosts);
        for (DiscoveryListener listener : listeners) {
            listener.hostsChanged(hosts);
        }
    }

    @Override
    public void start() {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(endpoint.hostname(), endpoint.port(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.open();

                Source source = new Source();
                source.setAddress("podsense");
                source.setFilter(labelFilter);
                ProtonReceiver receiver = connection.createReceiver("podsense");
                receiver.setSource(source);
                receiver.handler((protonDelivery, message) -> notifyListeners(decodeHosts(message)));
                receiver.open();
            }

        });
    }

    @SuppressWarnings("unchecked")
    private Set<Host> decodeHosts(Message message) {
        Set<Host> hosts = new HashSet<>();
        AmqpSequence sequence = (AmqpSequence) message.getBody();
        for (Object obj : sequence.getValue()) {
            Map<String, Object> podInfo = (Map<String, Object>) obj;
            String ip = (String) podInfo.get("ip");
            Map<String, Integer> reversedMap = new HashMap<>();
            Map<Integer, String> portMap = (Map<Integer, String>) podInfo.get("ports");
            for (Map.Entry<Integer, String> port : portMap.entrySet()) {
                reversedMap.put(port.getValue(), port.getKey());
            }
            hosts.add(new Host(ip, reversedMap));
        }
        return hosts;
    }
}
