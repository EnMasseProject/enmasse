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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Acts as an arbiter deciding in which broker a queue should run.
 */
public class QueueScheduler extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class.getName());

    private final KubernetesClient kubernetesClient;
    private final QueueState queueState = new QueueState();
    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final int port;

    public QueueScheduler(KubernetesClient kubernetesClient, int port) {
        this.kubernetesClient = kubernetesClient;
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
                        queueState.brokerAdded(conn.result().getRemoteContainer(), Artemis.create(vertx, connection).get());
                    } catch (Exception e) {
                        log.error("Error adding broker", e);
                    }
                });
            }).closeHandler(conn -> {
                executorService.execute(() -> queueState.brokerRemoved(conn.result().getRemoteContainer()));
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

        executorService.execute(() -> queueState.groupUpdated(groupId, configMap.getData().keySet()));
    }

    private void addressesDeleted(ConfigMap configMap) {
        String groupId = configMap.getMetadata().getLabels().get(LabelKeys.GROUP_ID);
        executorService.execute(() -> queueState.groupDeleted(groupId));
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        log.warn("Watcher closed", cause);
    }
}
