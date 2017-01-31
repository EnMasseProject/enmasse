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

import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Acts as an arbiter deciding in which broker a queue should run.
 */
public class QueueScheduler extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class.getName());

    private final KubernetesClient kubernetesClient;
    private final SchedulerState schedulerState = new SchedulerState();
    private final BrokerFactory brokerFactory;
    private final ExecutorService executorService;

    private final int port;

    public QueueScheduler(KubernetesClient kubernetesClient, ExecutorService executorService, BrokerFactory brokerFactory, int port) {
        this.kubernetesClient = kubernetesClient;
        this.executorService = executorService;
        this.brokerFactory = brokerFactory;
        this.port = port;
    }

    @Override
    public void start() {
        ProtonServer server = ProtonServer.create(vertx);
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
                log.info("Connection closed");
            }).disconnectHandler(protonConnection -> {
                connection.disconnect();
                log.info("Disconnected");
            }).open();
        });
        server.listen(port, event -> {
            if (event.succeeded()) {
                log.info("QueueScheduler is up and running on port " + port);
            } else {
                log.error("Error starting queue scheduler", event.cause());
            }
        });

        vertx.executeBlocking(promise -> {
            try {
                ConfigMapList configs = kubernetesClient.configMaps().withLabel("type", "address-config").list();
                for (ConfigMap config : configs.getItems()) {
                    eventReceived(Action.ADDED, config);
                }
                kubernetesClient.configMaps().withResourceVersion(configs.getMetadata().getResourceVersion()).watch(this);
                promise.complete(configs);
            } catch (Exception e) {
                promise.fail(e);
            }

        }, result -> {
            if (!result.succeeded()) {
                log.warn("Error getting deployment configs", result.cause());
            }
        });
    }


    @Override
    public void eventReceived(Action action, ConfigMap resource) {
        switch (action) {
            case ADDED:
                log.info("Deployment config was added");
                addressesChanged(resource);
                break;
            case DELETED:
                addressesDeleted(resource);
                break;
            case ERROR:
                log.error("Error with action", action);
                break;
            case MODIFIED:
                addressesChanged(resource);
                break;
        }
    }

    private void addressesChanged(ConfigMap configMap) {
        String groupId = configMap.getMetadata().getLabels().get(LabelKeys.GROUP_ID);

        executorService.execute(() -> {
            try {
                schedulerState.groupUpdated(groupId, configMap.getData().keySet());
            } catch (Exception e) {
                log.error("ERROR: ", e);
            }
        });
    }

    private void addressesDeleted(ConfigMap configMap) {
        String groupId = configMap.getMetadata().getLabels().get(LabelKeys.GROUP_ID);
        executorService.execute(() -> schedulerState.groupDeleted(groupId));
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        log.warn("Watcher closed", cause);
    }
}
