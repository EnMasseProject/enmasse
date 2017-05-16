package enmasse.controller.cert;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.config.LabelKeys;
import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for watchers of EnMasse instances
 */
public abstract class InstanceWatcher extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(InstanceWatcher.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    private Watch watch;
    private final OpenShiftClient client;

    protected abstract void instanceChanged(Instance instance) throws Exception;
    protected abstract void instanceDeleted(Instance instance) throws Exception;

    public InstanceWatcher(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public void start() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(LabelKeys.TYPE, "instance-config");
        ConfigMapList list = client.configMaps().withLabels(map).list();
        for (ConfigMap config : list.getItems()) {
            eventReceived(Action.ADDED, config);
        }
        watch = client.configMaps().withLabels(map).withResourceVersion(list.getMetadata().getResourceVersion()).watch(this);
    }

    @Override
    public void stop() {
        if (watch != null) {
            watch.close();
        }
    }

    @Override
    public void eventReceived(Action action, ConfigMap resource) {
        try {
            switch (action) {
                case ADDED:
                case MODIFIED:
                    instanceChanged(decodeInstance(resource));
                    break;
                case DELETED:
                    instanceDeleted(decodeInstance(resource));
                    break;
                case ERROR:
                    log.error("Error event " + action + " on " + resource);
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling event " + action + " on " + resource, e);
        }
    }

    private Instance decodeInstance(ConfigMap resource) throws IOException {
        return mapper.readValue(resource.getData().get("config.json"), enmasse.controller.api.v3.Instance.class).getInstance();
    }


    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("Received onClose for watcher", cause);
            stop();
            log.info("Watch for instance configs closed, recreating");
            start();
        } else {
            log.info("Watch for instance configs force closed, stopping");
        }
    }

}
