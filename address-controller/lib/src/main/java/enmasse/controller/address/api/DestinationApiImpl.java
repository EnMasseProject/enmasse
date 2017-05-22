package enmasse.controller.address.api;

import enmasse.config.AddressConfigKeys;
import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Destination;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.*;

public class DestinationApiImpl implements DestinationApi {

    private final OpenShiftClient client;
    private final InstanceId instanceId;

    public DestinationApiImpl(OpenShiftClient client, InstanceId instanceId) {
        this.client = client;
        this.instanceId = instanceId;
    }

    @Override
    public Optional<Destination> getDestinationWithAddress(String address) {
        ConfigMap map = client.configMaps().inNamespace(instanceId.getNamespace()).withName(Kubernetes.sanitizeName(Kubernetes.sanitizeName("address-config-" + address))).get();
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
        replaceDestination(destination);
    }

    @Override
    public void replaceDestination(Destination destination) {
        String name = Kubernetes.sanitizeName("address-config-" + destination.address());
        DoneableConfigMap builder = client.configMaps().inNamespace(instanceId.getNamespace()).createOrReplaceWithNew()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.GROUP_ID, Kubernetes.sanitizeName(destination.group()))
                .addToLabels(LabelKeys.TYPE, "address-config")
                .addToLabels(LabelKeys.INSTANCE, Kubernetes.sanitizeName(instanceId.getId()))
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
}
