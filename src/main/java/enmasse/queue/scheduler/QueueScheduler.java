package enmasse.queue.scheduler;

import enmasse.address.controller.admin.OpenShiftHelper;
import enmasse.address.controller.model.LabelKeys;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Acts as an arbiter deciding in which broker a queue should run.
 */
public class QueueScheduler extends AbstractVerticle implements Watcher<DeploymentConfig> {
    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class.getName());

    private final OpenShiftClient openShiftClient;
    private final QueueState queueState = new QueueState();
    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final int port;

    public QueueScheduler(OpenShiftClient openShiftClient, int port) {
        this.openShiftClient = openShiftClient;
        this.port = port;
    }

    @Override
    public void start() {
        ProtonServer server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("queue-scheduler");
            connection.openHandler(conn -> {
                log.info("Connection opened from " + conn.result().getRemoteContainer());
                executorService.execute(() -> queueState.brokerAdded(new Artemis(connection)));
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
                DeploymentConfigList configs = openShiftClient.deploymentConfigs().withLabel("type", "address-config").list();
                for (DeploymentConfig config : configs.getItems()) {
                    eventReceived(Action.ADDED, config);
                }
                openShiftClient.deploymentConfigs().withResourceVersion(configs.getMetadata().getResourceVersion()).watch(this);
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
    public void eventReceived(Action action, DeploymentConfig resource) {
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

    private void addressesChanged(DeploymentConfig deploymentConfig) {
        String deploymentId = deploymentConfig.getMetadata().getName();
        Set<String> addresses = OpenShiftHelper.parseAddressAnnotation(deploymentConfig.getMetadata().getAnnotations().get(LabelKeys.ADDRESS_LIST));

        executorService.execute(() -> queueState.deploymentUpdated(deploymentId, addresses));
    }

    private void addressesDeleted(DeploymentConfig deploymentConfig) {
        executorService.execute(() -> queueState.deploymentDeleted(deploymentConfig.getMetadata().getName()));
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        log.warn("Watcher closed", cause);
    }
}
