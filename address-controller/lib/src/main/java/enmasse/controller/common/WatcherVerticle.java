package enmasse.controller.common;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A verticle that handles watching a resource with the appropriate reconnect and retry logic,
 * which notifies a resource interface when things change.
 */
public class WatcherVerticle<T> extends AbstractVerticle implements io.fabric8.kubernetes.client.Watcher {
    private static final Logger log = LoggerFactory.getLogger(WatcherVerticle.class.getName());
    private final Random random;
    private Watch watch;
    private final Resource<T> resource;
    private final Watcher<T> changeHandler;

    public WatcherVerticle(Resource<T> resource, Watcher<T> changeHandler) {
        this.random = new Random(System.currentTimeMillis());
        this.resource = resource;
        this.changeHandler = changeHandler;
    }

    private long nextCheck() {
        return 5000 + Math.abs(random.nextLong()) % 10000;
    }

    public void start() {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                changeHandler.resourcesUpdated(resource.listResources());
                promise.complete(resource.watchResources(this));
                 //client.configMaps().inNamespace(namespace).withLabels(labels).watch(this));
                vertx.setTimer(nextCheck(), id -> checkResources());
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                watch = result.result();
            } else {
                log.error("Error starting watch", result.cause());
            }
        });
    }

    /*private Set<T> listConfigs() {
        return decoder.decodeConfigs(client.configMaps().inNamespace(namespace).withLabels(labels).list().getItems());
    }*/

    private void checkResources() {
        vertx.executeBlocking(promise -> {
            try {
                changeHandler.resourcesUpdated(resource.listResources());
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.failed()) {
                log.warn("Error checking instances", result.cause());
            }
            vertx.setTimer(nextCheck(), id -> checkResources());
        });
    }


    public void stop() {
        if (watch != null) {
            watch.close();
        }
    }

    @Override
    public void eventReceived(Action action, Object obj) {
        if (action.equals(Action.ERROR)) {
            log.error("Got error event while watching resource " + obj);
            return;
        }

        try {
            switch (action) {
                case MODIFIED:
                case ADDED:
                case DELETED:
                    changeHandler.resourcesUpdated(resource.listResources());
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling event", e);
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("Received onClose for instance config resource", cause);
            stop();
            start();
        } else {
            log.info("Watch for instance configs force closed, stopping");
            watch = null;
            stop();
        }
    }
}
