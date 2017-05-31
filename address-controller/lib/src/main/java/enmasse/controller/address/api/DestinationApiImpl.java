package enmasse.controller.address.api;

import enmasse.config.AddressConfigKeys;
import enmasse.config.LabelKeys;
import enmasse.config.AnnotationKeys;
import enmasse.controller.common.*;
import enmasse.controller.model.Destination;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DestinationApiImpl implements DestinationApi {

    private static final Logger log = LoggerFactory.getLogger(DestinationApiImpl.class);
    private final Vertx vertx;
    private final OpenShiftClient client;
    private final InstanceId instanceId;

    public DestinationApiImpl(Vertx vertx, OpenShiftClient client, InstanceId instanceId) {
        this.vertx = vertx;
        this.client = client;
        this.instanceId = instanceId;
    }

    @Override
    public Optional<Destination> getDestinationWithAddress(String address) {
        ConfigMap map = client.configMaps().inNamespace(instanceId.getNamespace()).withName(Kubernetes.sanitizeName("address-config-" + address)).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getDestinationFromConfig(map));
        }
    }

    @Override
    public Optional<Destination> getDestinationWithUuid(String uuid) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        labels.put(LabelKeys.UUID, uuid);

        ConfigMapList list = client.configMaps().inNamespace(instanceId.getNamespace()).withLabels(labels).list();
        if (list.getItems().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(getDestinationFromConfig(list.getItems().get(0)));
        }
    }

    public Destination getDestinationFromConfig(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();

        Destination.Builder destBuilder = new Destination.Builder(data.get(AddressConfigKeys.ADDRESS), data.get(AddressConfigKeys.GROUP_ID));
        destBuilder.storeAndForward(Boolean.parseBoolean(data.get(AddressConfigKeys.STORE_AND_FORWARD)));
        destBuilder.multicast(Boolean.parseBoolean(data.get(AddressConfigKeys.MULTICAST)));
        destBuilder.flavor(Optional.ofNullable(data.get(AddressConfigKeys.FLAVOR)));
        destBuilder.uuid(Optional.ofNullable(data.get(AddressConfigKeys.UUID)));
        destBuilder.status(new Destination.Status(Boolean.parseBoolean(data.get(AddressConfigKeys.READY))));
        return destBuilder.build();
    }

    @Override
    public Set<Destination> listDestinations() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");

        Set<Destination> destinations = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().inNamespace(instanceId.getNamespace()).withLabels(labels).list();
        for (ConfigMap config : list.getItems()) {
            destinations.add(getDestinationFromConfig(config));
        }
        return destinations;
    }

    @Override
    public void createDestination(Destination destination) {
        createOrReplace(destination);
    }

    @Override
    public void replaceDestination(Destination destination) {
        String name = Kubernetes.sanitizeName("address-config-" + destination.address());
        ConfigMap previous = client.configMaps().inNamespace(instanceId.getNamespace()).withName(name).get();
        if (previous == null) {
            return;
        }
        createOrReplace(destination);
    }

    private void createOrReplace(Destination destination) {
        String name = Kubernetes.sanitizeName("address-config-" + destination.address());
        DoneableConfigMap builder = client.configMaps().inNamespace(instanceId.getNamespace()).withName(name).createOrReplaceWithNew()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "address-config")
                .addToAnnotations(AnnotationKeys.GROUP_ID, destination.group())
                .addToAnnotations(AnnotationKeys.INSTANCE, instanceId.getId())
                .endMetadata();
        builder.addToData(AddressConfigKeys.ADDRESS, destination.address());
        builder.addToData(AddressConfigKeys.GROUP_ID, destination.group());
        builder.addToData(AddressConfigKeys.STORE_AND_FORWARD, String.valueOf(destination.storeAndForward()));
        builder.addToData(AddressConfigKeys.MULTICAST, String.valueOf(destination.multicast()));
        builder.addToData(AddressConfigKeys.READY, String.valueOf(destination.status().isReady()));
        destination.flavor().ifPresent(f -> builder.addToData(AddressConfigKeys.FLAVOR, f));
        destination.uuid().ifPresent(f -> builder.addToData(AddressConfigKeys.UUID, f));
        builder.done();
    }

    @Override
    public void deleteDestination(Destination destination) {
        String name = Kubernetes.sanitizeName("address-config-" + destination.address());
        client.configMaps().inNamespace(instanceId.getNamespace()).withName(name).delete();
    }

    @Override
    public Watch watchDestinations(Watcher<Destination> watcher) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        WatcherVerticle<Destination> verticle = new WatcherVerticle<>(new Resource<Destination>() {
            @Override
            public io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher w) {
                return client.configMaps().inNamespace(instanceId.getNamespace()).withLabels(labels).watch(w);
            }

            @Override
            public Set<Destination> listResources() {
                return listDestinations();
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
}
