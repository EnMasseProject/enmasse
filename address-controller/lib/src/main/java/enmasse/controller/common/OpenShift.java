package enmasse.controller.common;

import enmasse.controller.model.Destination;
import enmasse.controller.model.InstanceId;
import enmasse.controller.address.DestinationCluster;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.openshift.client.ParameterValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for OpenShift operations done by the address controller
 */
public interface OpenShift {

    static String sanitizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    static void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }

    InstanceId getInstanceId();
    OpenShift mutateClient(InstanceId instance);

    List<DestinationCluster> listClusters();
    void updateDestinations(Set<Destination> destinations);
    void create(KubernetesList resources);
    void delete(KubernetesList resources);
    ConfigMap createAddressConfig(Set<Destination> destinations);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    Namespace createNamespace(InstanceId instance);
    List<Namespace> listNamespaces(Map<String, String> labelMap);
    void deleteNamespace(String namespace);

    void addDefaultViewPolicy(InstanceId instance);

    List<Route> getRoutes(InstanceId instanceId);

    boolean hasService(String service);
}
