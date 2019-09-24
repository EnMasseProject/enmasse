/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.InfraConfigs;
import io.enmasse.k8s.util.Templates;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the Kubernetes client and adds some helper methods.
 */
public class KubernetesHelper implements Kubernetes {
    private static final String TEMPLATE_SUFFIX = ".yaml";

    private final NamespacedKubernetesClient client;
    private final String namespace;
    private final File templateDir;
    private final boolean isOpenShift;

    public KubernetesHelper(String namespace, NamespacedKubernetesClient client, File templateDir, boolean isOpenShift) {
        this.client = client;
        this.namespace = namespace;
        this.templateDir = templateDir;
        this.isOpenShift = isOpenShift;
    }

    @Override
    public void create(KubernetesList resources) {
        client.lists().inNamespace(namespace).create(resources);
    }

    @Override
    public void apply(KubernetesList resources, boolean patchPersistentVolumeClaims) {
        for (HasMetadata resource : resources.getItems()) {
            try {
                if (resource instanceof ConfigMap) {
                    client.configMaps().withName(resource.getMetadata().getName()).createOrReplace((ConfigMap) resource);
                } else if (resource instanceof Secret) {
                    client.secrets().withName(resource.getMetadata().getName()).createOrReplace((Secret) resource);
                } else if (resource instanceof Deployment) {
                    client.apps().deployments().withName(resource.getMetadata().getName()).patch((Deployment) resource);
                } else if (resource instanceof StatefulSet) {
                    client.apps().statefulSets().withName(resource.getMetadata().getName()).cascading(false).patch((StatefulSet) resource);
                } else if (resource instanceof Service) {
                    client.services().withName(resource.getMetadata().getName()).createOrReplace((Service) resource);
                } else if (resource instanceof NetworkPolicy) {
                    client.network().networkPolicies().withName(resource.getMetadata().getName()).createOrReplace((NetworkPolicy) resource);
                } else if (resource instanceof PersistentVolumeClaim && patchPersistentVolumeClaims) {
                    client.persistentVolumeClaims().withName(resource.getMetadata().getName()).replace((PersistentVolumeClaim) resource);
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 404) {
                    // Create it if it does not exist
                    client.resource(resource).createOrReplace();
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public KubernetesList processTemplate(String templateName, Map<String, String> parameters) {
        File templateFile = new File(templateDir, templateName + TEMPLATE_SUFFIX);
        return Templates.process(templateFile, parameters);
    }

    @Override
    public Set<Deployment> getReadyDeployments(AddressSpace addressSpace) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        return client.apps().deployments().inNamespace(namespace).withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems().stream()
                .filter(KubernetesHelper::isReady)
                .collect(Collectors.toSet());
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment");  // TODO: is there an existing constant for this somewhere?
    }

    @Override
    public Set<StatefulSet> getReadyStatefulSets(AddressSpace addressSpace) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        return client.apps().statefulSets().inNamespace(namespace).withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems().stream()
                .filter(KubernetesHelper::isReady)
                .collect(Collectors.toSet());
    }

    public static boolean isStatefulSet(HasMetadata res) {
        return res.getKind().equals("StatefulSet");  // TODO: is there an existing constant for this somewhere?
    }

    @Override
    public void deleteResourcesNotIn(String [] uuids) {
        client.apps().statefulSets().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).withPropagationPolicy("Background").delete();
        client.secrets().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).withPropagationPolicy("Background").delete();
        client.configMaps().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).withPropagationPolicy("Background").delete();
        client.apps().deployments().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).withPropagationPolicy("Background").delete();
        client.services().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).withPropagationPolicy("Background").delete();
        client.persistentVolumeClaims().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        if (isOpenShift) {
            client.adapt(OpenShiftClient.class).routes().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).withPropagationPolicy("Background").delete();
        }
    }

    @Override
    public Optional<Secret> getSecret(String secretName) {
        return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(secretName).get());
    }

    private static boolean isReady(Deployment deployment) {
        // TODO: Assuming at least one replica is ok
        Integer readyReplicas = deployment.getStatus().getReadyReplicas();
        return readyReplicas != null && readyReplicas >= 1;
    }

    private static boolean isReady(StatefulSet statefulSet) {
        // TODO: Assuming at least one replica is ok
        Integer readyReplicas = statefulSet.getStatus().getReadyReplicas();
        return readyReplicas != null && readyReplicas >= 1;
    }

    @Override
    public boolean existsAddressSpace(AddressSpace addressSpace) {
        return client.services().inNamespace(namespace).withName(KubeUtil.getAddressSpaceServiceName("messaging", addressSpace)).get() != null;
    }

    @Override
    public String getAppliedPlan(AddressSpace addressSpace) {
        if (addressSpace.getAnnotation(AnnotationKeys.APPLIED_PLAN) != null) {
            return addressSpace.getAnnotation(AnnotationKeys.APPLIED_PLAN);
        }
        Service messaging = client.services().inNamespace(namespace).withName(KubeUtil.getAddressSpaceServiceName("messaging", addressSpace)).get();
        if (messaging == null) {
            return null;
        }
        if (messaging.getMetadata().getAnnotations() == null) {
            return null;
        }
        return messaging.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_PLAN);
    }

    @Override
    public InfraConfig getAppliedInfraConfig(AddressSpace addressSpace) throws IOException {
        InfraConfig config = InfraConfigs.parseCurrentInfraConfig(addressSpace);
        if (config != null) {
            return config;
        }

        Service messaging = client.services().inNamespace(namespace).withName(KubeUtil.getAddressSpaceServiceName("messaging", addressSpace)).get();
        if (messaging == null) {
            return null;
        }

        if (messaging.getMetadata().getAnnotations() == null) {
            return null;
        }

        return InfraConfigs.parseCurrentInfraConfig(messaging.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_INFRA_CONFIG));
    }
}
