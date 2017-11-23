package io.enmasse.controller.common;

import io.enmasse.address.model.Endpoint;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.time.Clock;
import java.util.*;

/**
 * Interface for Kubernetes operations done by the address controller
 */
public interface Kubernetes {

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

    void createNamespace(String name, String namespace);
    List<Namespace> listNamespaces();
    void deleteNamespace(String namespace);

    boolean existsNamespace(String namespace);

    boolean hasService(String service);

    void createEndpoint(Endpoint endpoint, Service service, String addressSpaceName, String namespace);

    Set<Deployment> getReadyDeployments();

    boolean isDestinationClusterReady(String clusterId);

    List<Pod> listRouters();

    Optional<Secret> getSecret(String secretName);

    TokenReview performTokenReview(String token);

    SubjectAccessReview performSubjectAccessReview(String user, String namespace, String verb);

    boolean isRBACSupported();
    void addAddressAdminRole(String namespace);
    void addInfraViewRole(String controllerNamespace, String namespace);
    void addSystemImagePullerPolicy(String namespace, String tenantNamespace);

    EventLogger createEventLogger(Clock clock, String componentName);
}
