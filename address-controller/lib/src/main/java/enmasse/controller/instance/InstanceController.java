package enmasse.controller.instance;

import enmasse.config.AnnotationKeys;
import enmasse.controller.common.Watch;
import enmasse.controller.common.Watcher;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.instance.cert.CertManager;
import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The instance controller is responsible for watching instance that should be created, as well as
 * propagating any relevant status for a given instance back to the instance resource.
 */

public class InstanceController extends AbstractVerticle implements Watcher<Instance> {
    private static final Logger log = LoggerFactory.getLogger(InstanceController.class.getName());
    private final OpenShiftClient client;

    private final CertManager certManager;
    private final InstanceManager instanceManager;
    private final InstanceApi instanceApi;
    private Watch watch;

    public InstanceController(InstanceManager instanceManager, OpenShiftClient client, InstanceApi instanceApi, CertManager certManager) {
        this.instanceManager = instanceManager;
        this.client = client;
        this.instanceApi = instanceApi;
        this.certManager = certManager;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(instanceApi.watchInstances(this));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                this.watch = result.result();
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        vertx.executeBlocking(promise -> {
            try {
                if (watch != null) {
                    watch.close();
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                stopFuture.complete();
            } else {
                stopFuture.fail(result.cause());
            }
        });
    }

    @Override
    public synchronized void resourcesUpdated(Set<Instance> instances) throws Exception {
        log.debug("Check instances in instance controller: " + instances);
        createInstances(instances);
        retainInstances(instances);

        for (Instance instance : instanceApi.listInstances()) {
            certManager.updateCerts(instance);
            Instance.Builder mutableInstance = new Instance.Builder(instance);
            updateReadiness(mutableInstance);
            updateRoutes(mutableInstance);
            instanceApi.replaceInstance(mutableInstance.build());
        }
    }

    private void retainInstances(Set<Instance> desiredInstances) {
        instanceManager.retainInstances(desiredInstances.stream().map(Instance::id).collect(Collectors.toSet()));
    }

    private void createInstances(Set<Instance> instances) {
        for (Instance instance : instances) {
            instanceManager.create(instance);
        }
    }

    private void updateRoutes(Instance.Builder instance) throws IOException {
        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.INSTANCE, instance.id().getId());

        /* Watch for routes and ingress */
        if (client.isAdaptable(OpenShiftClient.class)) {
            client.routes().inNamespace(instance.id().getNamespace()).list().getItems().stream()
                    .filter(route -> isPartOfInstance(instance.id().getId(), route))
                    .forEach(route -> updateRoute(instance, route.getMetadata().getName(), route.getSpec().getHost()));
        } else {
            client.extensions().ingresses().inNamespace(instance.id().getNamespace()).list().getItems().stream()
                    .filter(ingress -> isPartOfInstance(instance.id().getId(), ingress))
                    .forEach(ingress -> updateRoute(instance, ingress.getMetadata().getName(), ingress.getSpec().getRules().get(0).getHost()));
        }
    }

    private static boolean isPartOfInstance(String id, HasMetadata resource) {
        return resource.getMetadata().getAnnotations() != null && id.equals(resource.getMetadata().getAnnotations().get(AnnotationKeys.INSTANCE));
    }

    private void updateRoute(Instance.Builder builder, String name, String host) {
        log.debug("Updating routes for " + name + " to " + host);
        if ("messaging".equals(name)) {
            builder.messagingHost(Optional.ofNullable(host));
        } else if ("mqtt".equals(name)) {
            builder.mqttHost(Optional.ofNullable(host));
        } else if ("console".equals(name)) {
            builder.consoleHost(Optional.ofNullable(host));
        }
    }

    private void updateReadiness(Instance.Builder mutableInstance) {
        Instance instance = mutableInstance.build();
        boolean isReady = instanceManager.isReady(instance);
        if (instance.status().isReady() != isReady) {
            mutableInstance.status(new Instance.Status(isReady));
        }
    }
}
