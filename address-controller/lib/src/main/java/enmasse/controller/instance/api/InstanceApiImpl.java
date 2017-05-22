package enmasse.controller.instance.api;

import enmasse.config.LabelKeys;
import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.address.api.DestinationApiImpl;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the Instance API towards Kubernetes
 */
public class InstanceApiImpl implements InstanceApi {
    private final OpenShiftClient client;

    public InstanceApiImpl(OpenShiftClient client) {
        this.client = client;
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
    public Optional<Instance> getInstanceWithUuid(String uuid) {
        ConfigMapList list = client.configMaps().withLabel(LabelKeys.UUID, uuid).list();
        if (list.getItems().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(getInstanceFromConfig(list.getItems().get(0)));
        }
    }

    @Override
    public void createInstance(Instance instance) {
        replaceInstance(instance);
    }

    @Override
    public void replaceInstance(Instance instance) {
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
    public DestinationApi withInstance(InstanceId id) {
        return new DestinationApiImpl(client, id);
    }
}
