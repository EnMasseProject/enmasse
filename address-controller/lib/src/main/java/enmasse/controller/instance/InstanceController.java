package enmasse.controller.instance;

import enmasse.config.LabelKeys;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.Operation;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;


/**
 * The instance controller is responsible for watching instance that should be created, as well as
 * propagating any relevant status for a given instance back to the instance resource.
 */

public class InstanceController extends AbstractVerticle implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(InstanceController.class.getName());
    private Watch configWatch;
    private final Map<InstanceId, Watch> instanceWatches = new HashMap<>();
    private final OpenShiftClient client;
    private final InstanceManager instanceManager;
    private final InstanceApi instanceApi;

    public InstanceController(InstanceManager instanceManager, OpenShiftClient client, InstanceApi instanceApi) {
        this.instanceManager = instanceManager;
        this.client = client;
        this.instanceApi = instanceApi;
    }

    public void start() {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(LabelKeys.TYPE, "instance-config");
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                vertx.setPeriodic(5000, id -> checkInstances());
                promise.complete(startWatch(client.configMaps(), labelMap, client.getNamespace()));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                configWatch = result.result();
            } else {
                log.error("Error starting watch", result.cause());
            }
        });
    }

    public void stop() {
        Iterator<Map.Entry<InstanceId, Watch>> it = instanceWatches.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<InstanceId, Watch> entry = it.next();
            entry.getValue().close();
            it.remove();
        }

        if (configWatch != null) {
            configWatch.close();
        }
    }


    @Override
    public void eventReceived(Action action, Object resource) {
        if (action.equals(Action.ERROR)) {
            log.error("Got error event while watching resource " + resource);
            return;
        }

        try {
            if (resource instanceof Route) {
                routeEventReceived(action, (Route) resource);
            } else if (resource instanceof Ingress) {
                ingressEventReceived(action, (Ingress) resource);
            } else if (resource instanceof ConfigMap) {
                configEventReceived(action, (ConfigMap) resource);
            }
        } catch (Exception e) {
            log.error("Error handling event", e);
        }
    }

    private Instance getInstance(String name) throws IOException {
        return instanceApi.getInstanceWithId(InstanceId.withId(name)).get();
    }

    private void putInstance(Instance instance) {
        instanceApi.replaceInstance(instance);
    }

    private void routeEventReceived(Action action, Route route) throws Exception {
        switch (action) {
            case ADDED:
            case MODIFIED:
                Instance.Builder builder = new Instance.Builder(getInstance(route.getMetadata().getLabels().get(LabelKeys.INSTANCE)));
                updateRoute(builder, route.getMetadata().getName(), route.getSpec().getHost());
                putInstance(builder.build());
                break;
            case DELETED:
            case ERROR:
                log.info("Unhandled event " + action + " on route");
                break;
        }
    }

    private void updateRoute(Instance.Builder builder, String name, String host) {
        log.info("Updating routes for " + name + " to " + host);
        if ("messaging".equals(name)) {
            builder.messagingHost(Optional.of(host));
        } else if ("mqtt".equals(name)) {
            builder.mqttHost(Optional.of(host));
        } else if ("console".equals(name)) {
            builder.consoleHost(Optional.of(host));
        }
    }

    private void ingressEventReceived(Action action, Ingress ingress) throws Exception {
        switch (action) {
            case ADDED:
            case MODIFIED:
                Instance.Builder builder = new Instance.Builder(getInstance(ingress.getMetadata().getLabels().get(LabelKeys.INSTANCE)));
                updateRoute(builder, ingress.getMetadata().getName(), ingress.getSpec().getRules().get(0).getHost());
                putInstance(builder.build());
                break;
            case DELETED:
            case ERROR:
                log.info("Unhandled event " + action + " on ingress");
                break;
        }
    }

    private void configEventReceived(Action action, ConfigMap resource) throws Exception {
        if (action.equals(Action.ADDED)) {
            Instance instance = instanceApi.getInstanceFromConfig(resource);
            instanceManager.create(instance);
            watchInstance(instance);
        } else if (action.equals(Action.MODIFIED)) {
            // We are the ones modifying, so ignore
        } else if (action.equals(Action.DELETED)) {
            Instance instance = instanceApi.getInstanceFromConfig(resource);
            unwatchInstance(instance);
            instanceManager.delete(instance);
        }

    }

    private void watchInstance(Instance instance) {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(LabelKeys.INSTANCE, instance.id().getId());

        /* Watch for routes and ingress */
        if (client.isAdaptable(OpenShiftClient.class)) {
            instanceWatches.put(instance.id(), startWatch(client.routes(), labelMap, instance.id().getNamespace()));
        } else {
            instanceWatches.put(instance.id(), startWatch(client.extensions().ingresses(), labelMap, instance.id().getNamespace()));
        }
    }

    private void checkInstances() {
        vertx.executeBlocking(promise -> {
            try {
                for (Instance instance : instanceApi.listInstances()) {
                    boolean isReady = instanceManager.isReady(instance);
                    if (instance.status().isReady() != isReady) {
                        Instance.Builder updated = new Instance.Builder(instance);
                        updated.status(new Instance.Status(isReady));
                        putInstance(updated.build());
                    }
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.failed()) {
                log.warn("Error checking instances", result.cause());
            }
        });
    }

    private void unwatchInstance(Instance instance) {
        if (instanceWatches.containsKey(instance.id())) {
            instanceWatches.get(instance.id()).close();
            instanceWatches.remove(instance.id());
        }
    }

    private Watch startWatch(Operation<? extends HasMetadata, ?, ?, ?> operation, Map<String, String> labelMap, String namespace) {
        KubernetesResourceList resourceList  = (KubernetesResourceList) operation.inNamespace(namespace).withLabels(labelMap).list();

        for (Object item : resourceList.getItems()) {
            eventReceived(Action.ADDED, item);
        }
        return operation.withLabels(labelMap).withResourceVersion(resourceList.getMetadata().getResourceVersion()).watch(this);
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("Received onClose for instance config watcher", cause);
            stop();
            start();
        } else {
            log.info("Watch for instance configs force closed, stopping");
            configWatch = null;
            stop();
        }
    }
}
