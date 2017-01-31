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

package enmasse.queue.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonServer;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Acts as an arbiter deciding in which broker a queue should run.
 */
public class QueueScheduler extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class.getName());

    private final String configHost;
    private final int configPort;
    private final SchedulerState schedulerState = new SchedulerState();
    private final BrokerFactory brokerFactory;
    private final ExecutorService executorService;
    private volatile ProtonServer server;
    private volatile ProtonConnection configConnection;

    private final int port;

    public QueueScheduler(String configHost, int configPort, ExecutorService executorService, BrokerFactory brokerFactory, int listenPort) {
        this.configHost = configHost;
        this.configPort = configPort;
        this.executorService = executorService;
        this.brokerFactory = brokerFactory;
        this.port = listenPort;
    }

    @Override
    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("queue-scheduler");
            connection.openHandler(conn -> {
                log.info("Connection opened from " + conn.result().getRemoteContainer());
                executorService.execute(() -> {
                    try {
                        schedulerState.brokerAdded(conn.result().getRemoteContainer(), brokerFactory.createBroker(connection).get());
                    } catch (Exception e) {
                        log.error("Error adding broker", e);
                    }
                });
            }).closeHandler(conn -> {
                executorService.execute(() -> schedulerState.brokerRemoved(conn.result().getRemoteContainer()));
                connection.close();
                connection.disconnect();
                log.info("Broker connection closed");
            }).disconnectHandler(protonConnection -> {
                connection.disconnect();
                log.info("Broker connection disconnected");
            }).open();
        });
        server.listen(port, event -> {
            if (event.succeeded()) {
                log.info("QueueScheduler is up and running");
            } else {
                log.error("Error starting queue scheduler", event.cause());
            }
        });

        connectToConfigService(ProtonClient.create(vertx));
    }

    @Override
    public void stop() {
        if (configConnection != null) {
            configConnection.close();
        }

        if (server != null) {
            server.close();
        }
    }

    private void connectToConfigService(ProtonClient client) {
        client.connect(configHost, configPort, connResult -> {
            if (connResult.succeeded()) {
                log.info("Connected to the configuration service");
                configConnection = connResult.result();
                configConnection.open();

                ProtonReceiver receiver = configConnection.createReceiver("maas");
                receiver.handler((protonDelivery, message) -> {
                    String payload = (String)((AmqpValue)message.getBody()).getValue();
                    Map<String, Set<String>> addressConfig = decodeAddressConfig(new JsonObject(payload));
                    addressesChanged(addressConfig);
                });
                receiver.open();
            } else {
                log.error("Error connecting to configuration service", connResult.cause());
                vertx.setTimer(5000, id -> connectToConfigService(client));
            }
        });
    }

    private Map<String, Set<String>> decodeAddressConfig(JsonObject payload) {
        Map<String, Set<String>> addressMap = new LinkedHashMap<>();
        for (String group : payload.fieldNames()) {
            JsonObject groupObject = payload.getJsonObject(group);
            addressMap.put(group, new HashSet<>(groupObject.fieldNames()));
        }
        return addressMap;
    }

    private void addressesChanged(Map<String, Set<String>> addressMap) {
        executorService.execute(() -> {
            try {
                schedulerState.addressesChanged(addressMap);
            } catch (Exception e) {
                log.error("Error handling address change: ", e);
            }
        });
    }

    public int getPort() {
        if (server == null) {
            return 0;
        } else {
            return server.actualPort();
        }
    }
}
