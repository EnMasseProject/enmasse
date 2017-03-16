package enmasse.address.controller.admin;

import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.InstanceId;
import enmasse.address.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.ParameterValue;

import java.util.List;
import java.util.Map;

/**
 * Interface for OpenShift operations done by the address controller
 */
public interface OpenShift {

    static String sanitizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    InstanceId getInstanceId();
    List<DestinationCluster> listClusters();
    void updateDestinations(DestinationGroup destinationGroup);
    void create(KubernetesList resources);
    void delete(KubernetesList resources);
    ConfigMap createAddressConfig(DestinationGroup destinationGroup);
    Namespace createNamespace(InstanceId instance);
    OpenShift mutateClient(InstanceId instance);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);
    void addDefaultViewPolicy(InstanceId instance);
    List<Namespace> listNamespaces(Map<String, String> labelMap);
    List<Route> getRoutes(InstanceId instanceId);
    void deleteNamespace(String namespace);
    boolean hasService(String service);
}
