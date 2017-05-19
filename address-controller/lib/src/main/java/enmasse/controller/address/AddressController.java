package enmasse.controller.address;

import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Watches addresses
 */
public class AddressController extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final Kubernetes kubernetes;
    private final OpenShiftClient client;
    private final Map<InstanceId, String> addressSpaceMap = new HashMap<>();
    private Watch instanceWatch;

    public AddressController(Kubernetes kubernetes, OpenShiftClient client) {
        this.kubernetes = kubernetes;
        this.client = client;
    }

    @Override
    public void start() {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(LabelKeys.TYPE, "instance-config");
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(startWatch(labelMap, client.getNamespace()));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                instanceWatch = result.result();
            } else {
                log.error("Error starting watch", result.cause());
            }
        });
    }

    private Watch startWatch(Map<String, String> labelMap, String namespace) {
        ConfigMapList list = client.configMaps().inNamespace(namespace).withLabels(labelMap).list();
        for (ConfigMap item : list.getItems()) {
            eventReceived(Action.ADDED, item);
        }
        return client.configMaps().withLabels(labelMap).withResourceVersion(list.getMetadata().getResourceVersion()).watch(this);
    }

    @Override
    public void stop() {
        Iterator<Map.Entry<InstanceId, String>> it = addressSpaceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<InstanceId, String> entry = it.next();
            vertx.undeploy(entry.getValue());
            it.remove();
        }
        if (instanceWatch != null) {
            instanceWatch.close();
        }
    }

    @Override
    public void eventReceived(Action action, ConfigMap configMap) {
        switch (action) {
            case ADDED:
                Instance addedInstance = kubernetes.getInstanceFromConfig(configMap);
                if (!addressSpaceMap.containsKey(addedInstance.id())) {
                    AddressSpaceController addressSpaceController = new AddressSpaceController();
                    vertx.deployVerticle(addressSpaceController, result -> {
                        if (result.succeeded()) {
                            addressSpaceMap.put(addedInstance.id(), result.result());
                        } else {
                            log.warn("Unable to deploy address controller for " + addedInstance.id());
                        }
                    });
                }
                break;
            case DELETED:
                Instance deletedInstance = kubernetes.getInstanceFromConfig(configMap);
                if (addressSpaceMap.containsKey(deletedInstance.id())) {
                    vertx.undeploy(addressSpaceMap.get(deletedInstance.id()));
                }
                break;
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("Received onClose for instance config watcher", cause);
            stop();
            start();
        } else {
            log.info("Watch for instance configs force closed, stopping");
            instanceWatch = null;
            stop();
        }
    }
}
