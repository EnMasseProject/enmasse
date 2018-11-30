/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the Kubernetes client and adds some helper methods.
 */
public class KubernetesHelper implements Kubernetes {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(KubernetesHelper.class.getName());
    private static final String TEMPLATE_SUFFIX = ".yaml";

    private final NamespacedOpenShiftClient client;
    private final String namespace;
    private final String controllerToken;
    private final File templateDir;
    private final boolean isOpenShift;

    public KubernetesHelper(String namespace, NamespacedOpenShiftClient client, String token, File templateDir, boolean isOpenShift) {
        this.client = client;
        this.namespace = namespace;
        this.controllerToken = token;
        this.templateDir = templateDir;
        this.isOpenShift = isOpenShift;
    }

    @Override
    public void create(KubernetesList resources) {
        client.lists().inNamespace(namespace).create(resources);
    }

    @Override
    public void apply(KubernetesList resources) {
        for (HasMetadata resource : resources.getItems()) {
            try {
                if (resource instanceof ConfigMap) {
                    client.configMaps().withName(resource.getMetadata().getName()).patch((ConfigMap) resource);
                } else if (resource instanceof Secret) {
                    client.secrets().withName(resource.getMetadata().getName()).patch((Secret) resource);
                } else if (resource instanceof Deployment) {
                    client.apps().deployments().withName(resource.getMetadata().getName()).patch((Deployment) resource);
                } else if (resource instanceof StatefulSet) {
                    client.apps().statefulSets().withName(resource.getMetadata().getName()).cascading(false).patch((StatefulSet) resource);
                } else if (resource instanceof Service) {
                    client.services().withName(resource.getMetadata().getName()).patch((Service) resource);
                } else if (resource instanceof ServiceAccount) {
                    client.serviceAccounts().withName(resource.getMetadata().getName()).patch((ServiceAccount) resource);
                } else if (resource instanceof NetworkPolicy) {
                    client.network().networkPolicies().withName(resource.getMetadata().getName()).patch((NetworkPolicy) resource);
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
    public KubernetesList processTemplate(String templateName, ParameterValue... parameterValues) {
        File templateFile = new File(templateDir, templateName + TEMPLATE_SUFFIX);
        return client.templates().load(templateFile).processLocally(parameterValues);
    }

    public Set<Deployment> getReadyDeployments() {
        return client.apps().deployments().inNamespace(namespace).list().getItems().stream()
                .filter(KubernetesHelper::isReady)
                .collect(Collectors.toSet());
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment");  // TODO: is there an existing constant for this somewhere?
    }

    @Override
    public void deleteResourcesNotIn(String [] uuids) {
        client.apps().statefulSets().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.secrets().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.configMaps().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.apps().deployments().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.serviceAccounts().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.services().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.persistentVolumeClaims().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        if (isOpenShift) {
            client.roleBindings().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
            client.deploymentConfigs().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
            client.routes().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        }
    }

    @Override
    public Optional<Secret> getSecret(String secretName) {
        return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(secretName).get());
    }

    private static boolean isReady(Deployment deployment) {
        Integer unavailableReplicas = deployment.getStatus().getUnavailableReplicas();
        return unavailableReplicas == null || unavailableReplicas == 0;
    }

    @Override
    public void ensureServiceAccountExists(AddressSpace addressSpace) {
        String saName = KubeUtil.getAddressSpaceSaName(addressSpace);

        if (client.serviceAccounts().inNamespace(namespace).withName(saName).get() == null) {
            Map<String, String> labels = new HashMap<>();
            labels.put(LabelKeys.INFRA_UUID, addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID));
            labels.put(LabelKeys.INFRA_TYPE, addressSpace.getType());

            client.serviceAccounts().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(saName)
                    .withLabels(labels)
                    .endMetadata()
                    .done();
            createRoleBinding(saName + "-admin", namespace, labels, "ClusterRole", "admin", Arrays.asList(new Subject("ServiceAccount", saName, namespace)));
            createRoleBinding(saName + "-admin-reader", namespace, labels, "Role", "enmasse.io:admin-reader", Arrays.asList(new Subject("ServiceAccount", saName, namespace)));
        }
    }

    @Override
    public boolean existsAddressSpace(AddressSpace addressSpace) {
        return client.services().inNamespace(namespace).withName(KubeUtil.getAddressSpaceServiceName("messaging", addressSpace)).get() != null;
    }

    private Map doRawHttpRequest(String path, String method, Map body, boolean errorOk) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        try {
            HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve(path);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + controllerToken)
                    .method(method, body != null ? RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsBytes(body)) : null);

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                try (ResponseBody responseBody = response.body()) {
                    String responseString = responseBody != null ? responseBody.string() : "{}";
                    if (response.isSuccessful()) {
                        return mapper.readValue(responseString, Map.class);
                    } else {
                        if (errorOk) {
                            return null;
                        } else {
                            String errorMessage = String.format("Error performing %s on %s: %d, %s", method, path, response.code(), responseString);
                            log.warn(errorMessage);
                            throw new RuntimeException(errorMessage);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createRoleBinding(String name, String namespace, Map<String, String> labelMap, String refKind, String refName, List<Subject> subjectList) {

        String apiVersion = "rbac.authorization.k8s.io/v1";
        String apiPath = "/apis/rbac.authorization.k8s.io/v1";

        Map<String, Object> body = new HashMap<>();

        body.put("kind", "RoleBinding");
        body.put("apiVersion", apiVersion);

        Map<String, Object>  metadata = new HashMap<>();
        metadata.put("name", name);
        metadata.put("namespace", namespace);
        Map<String, String> labels = new HashMap<>();
        for (Map.Entry<String, String> labelEntry : labelMap.entrySet()) {
            labels.put(labelEntry.getKey(), labelEntry.getValue());
        }
        metadata.put("labels", labels);
        body.put("metadata", metadata);

        Map<String, String> roleRef = new HashMap<>();
        roleRef.put("apiGroup", "rbac.authorization.k8s.io");
        roleRef.put("kind", refKind);
        roleRef.put("name", refName);
        body.put("roleRef", roleRef);

        List<Map<String, String>> subjects = new ArrayList<>();

        for (Subject subjectEntry : subjectList) {
            Map<String, String> subject = new HashMap<>();
            subject.put("kind", subjectEntry.getKind());
            subject.put("name", subjectEntry.getName());
            if (subjectEntry.getNamespace() != null) {
                subject.put("namespace", subjectEntry.getNamespace());
            }
            subjects.add(subject);
        }

        body.put("subjects", subjects);


        doRawHttpRequest(apiPath + "/namespaces/" + namespace + "/rolebindings", "POST", body, false);
    }

    private static class Subject {
        private final String kind;
        private final String name;
        private final String namespace;

        private Subject(String kind, String name, String namespace) {
            this.kind = kind;
            this.name = name;
            this.namespace = namespace;
        }

        public String getName() {
            return name;
        }

        public String getKind() {
            return kind;
        }

        public String getNamespace() {
            return namespace;
        }
    }

}
