package enmasse.controller.common;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Common functionality for monitoring instances
 */
public abstract class ConfigWatcher<T> extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(ConfigWatcher.class.getName());
    private final OpenShiftClient client;
    private final String namespace;
    private final Random random;
    private Watch configWatch;
    private final Map<String, String> labels;

    public ConfigWatcher(Map<String, String> labels, String namespace, OpenShiftClient client) {
        this.labels = labels;
        this.namespace = namespace;
        this.client = client;
        this.random = new Random(System.currentTimeMillis());
    }

    protected abstract void checkConfigs(Set<T> configs) throws Exception;
    protected abstract Set<T> listConfigs() throws Exception;

    private long nextCheck() {
        return 5000 + Math.abs(random.nextLong()) % 10000;
    }

    public void start() {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                checkConfigs(listConfigs());
                promise.complete(client.configMaps().inNamespace(namespace).withLabels(labels).watch(this));
                vertx.setTimer(nextCheck(), id -> checkConfigs());
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

    private void checkConfigs() {
        vertx.executeBlocking(promise -> {
            try {
                checkConfigs(listConfigs());
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.failed()) {
                log.warn("Error checking instances", result.cause());
            }
            vertx.setTimer(nextCheck(), id -> checkConfigs());
        });
    }


    public void stop() {
        if (configWatch != null) {
            configWatch.close();
        }
    }

    @Override
    public void eventReceived(Action action, ConfigMap resource) {
        if (action.equals(Action.ERROR)) {
            log.error("Got error event while watching resource " + resource);
            return;
        }

        try {
            switch (action) {
                case MODIFIED:
                case ADDED:
                case DELETED:
                    checkConfigs(listConfigs());
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling event", e);
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
            configWatch = null;
            stop();
        }
    }
}
