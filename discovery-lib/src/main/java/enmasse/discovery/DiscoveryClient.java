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
import io.vertx.core.Future;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class DiscoveryClient extends AbstractVerticle {
    private final Map<Symbol, Map<String, String>> sourceFilter;
    private final List<DiscoveryListener> listeners = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(DiscoveryClient.class.getName());
    private final Endpoint endpoint;
    private final String containerName;
    private final String address;
    private final String certDir;
    private Set<Host> currentHosts = new LinkedHashSet<>();
    private volatile ProtonConnection connection;

    public DiscoveryClient(Endpoint endpoint, String address, Map<String, String> labelFilter, Map<String, String> annotationFilter, String containerName, String certDir) {
        this.endpoint = endpoint;
        this.address = address;
        this.sourceFilter = toSymbolMap(labelFilter, annotationFilter);
        this.containerName = containerName;
        this.certDir = certDir;
    }

    public DiscoveryClient(String address, Map<String, String> labelFilter, Map<String, String> annotationFilter, String containerName, String certDir) {
        this(getEndpoint(), address, labelFilter, annotationFilter, containerName, certDir);
    }

    private static Endpoint getEndpoint() {
        String host = System.getenv("CONFIGURATION_SERVICE_HOST");
        String port = System.getenv("CONFIGURATION_SERVICE_PORT");
        return new Endpoint(host, Integer.parseInt(port));
    }

    private static Map<Symbol, Map<String, String>> toSymbolMap(Map<String, String> labelFilter, Map<String, String> annotationFilter) {
        Map<Symbol, Map<String, String>> symbolMap = new HashMap<>();
        symbolMap.put(Symbol.getSymbol("labels"), labelFilter);
        symbolMap.put(Symbol.getSymbol("annotations"), annotationFilter);
        return symbolMap;
    }

    public void addListener(DiscoveryListener listener) {
        this.listeners.add(listener);
    }

    private void notifyListeners(Set<Host> hosts) {
        if (currentHosts.equals(hosts)) {
            return;
        }
        currentHosts = new LinkedHashSet<>(hosts);
        log.debug("Received new set of hosts: " + hosts);
        for (DiscoveryListener listener : listeners) {
            listener.hostsChanged(hosts);
        }
    }

    private ProtonClientOptions createClientOptions()
    {
        ProtonClientOptions options = new ProtonClientOptions();

        if (certDir != null) {
            options.setSsl(true)
                    .addEnabledSaslMechanism("EXTERNAL")
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        }
        return options;
    }

    @Override
    public void start(Future<Void> startFuture) {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(createClientOptions(), endpoint.hostname(), endpoint.port(), event -> {
            if (event.succeeded()) {
                connection = event.result();
                connection.open();

                Source source = new Source();
                source.setAddress(address);
                source.setFilter(sourceFilter);
                ProtonReceiver receiver = connection.createReceiver(address);
                receiver.openHandler(o -> startFuture.complete());
                receiver.setSource(source);
                receiver.handler((protonDelivery, message) -> notifyListeners(decodeHosts(message)));
                receiver.open();
            }

        });
    }

    @SuppressWarnings("unchecked")
    private Set<Host> decodeHosts(Message message) {
        Set<Host> hosts = new HashSet<>();
        AmqpValue value = (AmqpValue) message.getBody();
        for (Object obj : (List)value.getValue()) {
            Map<String, Object> podInfo = (Map<String, Object>) obj;
            String host = (String) podInfo.get("host");
            String ready = (String) podInfo.get("ready");
            String phase = (String) podInfo.get("phase");
            if ("True".equals(ready) && "Running".equals(phase)) {
                Map<String, Map<String, Integer>> portMap = (Map<String, Map<String, Integer>>) podInfo.get("ports");
                if (containerName != null) {
                    hosts.add(new Host(host, portMap.get(containerName)));
                } else {
                    hosts.add(new Host(host, portMap.values().iterator().next()));
                }
            }
        }
        return hosts;
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        vertx.runOnContext(h -> {
            if (connection != null) {
                connection.close();
                stopFuture.complete();
            }
        });
    }
}
