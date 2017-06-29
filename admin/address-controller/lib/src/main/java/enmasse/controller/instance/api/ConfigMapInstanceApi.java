package enmasse.controller.instance.api;

import enmasse.config.LabelKeys;
import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.address.api.ConfigMapDestinationApi;
import enmasse.controller.common.*;
import enmasse.controller.common.Watch;
import enmasse.controller.common.Watcher;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the Instance API towards Kubernetes
 */
public class ConfigMapInstanceApi implements InstanceApi {
    private final OpenShiftClient client;
    private final Vertx vertx;

    public ConfigMapInstanceApi(Vertx vertx, OpenShiftClient client) {
        this.client = client;
        this.vertx = vertx;
    }

    @Override
    public Optional<Instance> getInstanceWithId(InstanceId instanceId) {
        ConfigMap map = client.configMaps().withName(Kubernetes.sanitizeName("instance-config-" + instanceId.getId())).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getInstanceFromConfig(map));
        }
    }

    @Override
    public void createInstance(Instance instance) {
        createOrReplace(instance);
    }

    @Override
    public void replaceInstance(Instance instance) {
        String name = Kubernetes.sanitizeName("instance-config-" + instance.id().getId());
        ConfigMap previous = client.configMaps().withName(name).get();
        if (previous == null) {
            return;
        }
        createOrReplace(instance);
    }

    public void createOrReplace(Instance instance) {
        String name = Kubernetes.sanitizeName("instance-config-" + instance.id().getId());
        client.configMaps().createOrReplaceWithNew()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "instance-config")
                .endMetadata()
                .addToData("config.json", enmasse.controller.instance.v3.Instance.toJson(instance))
                .done();
    }

    @Override
    public void deleteInstance(Instance instance) {
        String name = Kubernetes.sanitizeName("instance-config-" + instance.id().getId());
        client.configMaps().withName(name).delete();
    }

    @Override
    public Set<Instance> listInstances() {
        Set<Instance> instances = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().withLabel(LabelKeys.TYPE, "instance-config").list();
        for (ConfigMap map : list.getItems()) {
            instances.add(getInstanceFromConfig(map));
        }
        return instances;
    }

    @Override
    public Instance getInstanceFromConfig(ConfigMap map) {
        return enmasse.controller.instance.v3.Instance.fromJson(map.getData().get("config.json"));
    }

    @Override
    public Watch watchInstances(Watcher<Instance> watcher) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "instance-config");
        WatcherVerticle<Instance> verticle = new WatcherVerticle<>(new Resource<Instance>() {
            @Override
            public io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher w) {
                return client.configMaps().withLabels(labels).watch(w);
            }

            @Override
            public Set<Instance> listResources() {
                return listInstances();
            }
        }, watcher);

        CompletableFuture<String> promise = new CompletableFuture<>();
        vertx.deployVerticle(verticle, result -> {
            if (result.succeeded()) {
                promise.complete(result.result());
            } else {
                promise.completeExceptionally(result.cause());
            }
        });

        String id = promise.get(1, TimeUnit.MINUTES);
        return () -> vertx.undeploy(id);
    }

    @Override
    public DestinationApi withInstance(InstanceId id) {
        return new ConfigMapDestinationApi(vertx, client, id);
    }
}
