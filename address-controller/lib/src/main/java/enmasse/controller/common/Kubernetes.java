package enmasse.controller.common;

import enmasse.controller.address.DestinationCluster;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.openshift.client.ParameterValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for Kubernetes operations done by the address controller
 */
public interface Kubernetes {

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
    Kubernetes mutateClient(InstanceId instance);

    ConfigMap createAddressConfig(Destination destination);
    void deleteAddressConfig(Destination destination);

    ConfigMap getInstanceConfig(InstanceId instanceId);
    ConfigMap createInstanceConfig(Instance instance);
    void deleteInstanceConfig(Instance instance);

    List<DestinationCluster> listClusters();
    void create(HasMetadata ... resources);
    void create(KubernetesList resources);
    void delete(KubernetesList resources);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    Namespace createNamespace(InstanceId instance);
    List<Namespace> listNamespaces(Map<String, String> labelMap);
    void deleteNamespace(String namespace);

    void addDefaultViewPolicy(InstanceId instance);

    List<Route> getRoutes(InstanceId instanceId);

    boolean hasService(String service);
    String createInstanceSecret(InstanceId instanceId) throws IOException;

    boolean isDestinationReady(Destination destination);
}
