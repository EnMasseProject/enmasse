package enmasse.controller.common;

import io.enmasse.address.model.AuthenticationServiceResolver;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.types.AddressSpaceType;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;

/**
 * Interface for Kubernetes operations done by the address controller
 */
public interface Kubernetes {

    static String sanitizeName(String name) {
        String replaced = name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        if (replaced.startsWith("-")) {
            replaced = replaced.replaceFirst("-", "1");
        }
        if (replaced.endsWith("-")) {
            replaced = replaced.substring(0, replaced.length() - 2) + "1";
        }
        return replaced;
    }

    static void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            if (labels == null) {
                labels = new LinkedHashMap<>();
            }
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }

    static void addObjectAnnotation(KubernetesList items, String annotationKey, String annotationValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> annotations = item.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new LinkedHashMap<>();
            }
            annotations.put(annotationKey, annotationValue);
            item.getMetadata().setAnnotations(annotations);
        }
    }

    String getNamespace();
    Kubernetes withNamespace(String namespace);

    List<AddressCluster> listClusters();
    void create(HasMetadata ... resources);
    void create(KubernetesList resources);
    void create(KubernetesList resources, String namespace);
    void delete(KubernetesList resources);
    void delete(HasMetadata ... resources);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    Namespace createNamespace(String name, String namespace);

    void deleteNamespace(String namespace);

    void addDefaultViewPolicy(String namespace);

    boolean hasService(String service);
    void createSecretWithDefaultPermissions(String secretName, String namespace);
    void createEndpoint(Endpoint endpoint, Map<String, String> servicePortMap, String addressSpaceName, String namespace);

    Set<Deployment> getReadyDeployments();

    boolean isDestinationClusterReady(String clusterId);

    List<Namespace> listNamespaces(Map<String, String> labels);

    List<Pod> listRouters();
}
