package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.LabelKeys;
import enmasse.storage.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps the OpenShift client and adds some helper methods.
 */
public class OpenShiftHelper {
    private static final Logger log = LoggerFactory.getLogger(OpenShiftHelper.class.getName());
    private final OpenShiftClient client;

    public OpenShiftHelper(OpenShiftClient client) {
        this.client = client;
    }

    public OpenShiftClient getClient() {
        return client;
    }

    public List<DestinationCluster> listClusters() {
        Map<Destination, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a storage cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.deploymentConfigs().list().getItems());
        objects.addAll(client.persistentVolumeClaims().list().getItems());
        objects.addAll(client.configMaps().list().getItems());
        objects.addAll(client.replicationControllers().list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> labels = config.getMetadata().getLabels();
            if (labels != null && labels.containsKey(LabelKeys.ADDRESS)) {
                String address = labels.get(LabelKeys.ADDRESS);
                String type = labels.get(LabelKeys.ADDRESS_TYPE);
                String flavor = labels.get(LabelKeys.FLAVOR);
                Destination destination = new Destination(address, !flavor.isEmpty(), AddressType.TOPIC.name().equals(type), flavor);
                if (!resourceMap.containsKey(destination)) {
                    resourceMap.put(destination, new ArrayList<>());
                }
                resourceMap.get(destination).add(config);
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> {
                    KubernetesList list = new KubernetesList();
                    list.setItems(entry.getValue());
                    return new DestinationCluster(client, entry.getKey(), list);
                }).collect(Collectors.toList());
    }
}
