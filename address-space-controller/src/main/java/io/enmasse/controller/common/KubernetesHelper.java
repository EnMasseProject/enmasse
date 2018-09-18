/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
    private static final Logger log = LoggerFactory.getLogger(KubernetesHelper.class.getName());
    private static final String TEMPLATE_SUFFIX = ".yaml";

    private final NamespacedOpenShiftClient client;
    private final String namespace;
    private final String controllerToken;
    private final File templateDir;

    public KubernetesHelper(String namespace, NamespacedOpenShiftClient client, String token, File templateDir) {
        this.client = client;
        this.namespace = namespace;
        this.controllerToken = token;
        this.templateDir = templateDir;
    }

    @Override
    public void create(KubernetesList resources) {
        client.lists().inNamespace(namespace).create(resources);
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

    @Override
    public boolean hasService(String service) {
        return client.services().inNamespace(namespace).withName(service).get() != null;
    }

    @Override
    public boolean hasService(String infraUuid, String service) {
        return client.services().inNamespace(namespace).withName(service + "-" + infraUuid).get() != null;
    }

    public Set<Deployment> getReadyDeployments() {
        return client.extensions().deployments().inNamespace(namespace).list().getItems().stream()
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
        client.deploymentConfigs().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.extensions().deployments().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.serviceAccounts().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.services().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        client.persistentVolumeClaims().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        if (client.isAdaptable(OpenShiftClient.class)) {
            client.routes().withLabel(LabelKeys.INFRA_TYPE).withLabelNotIn(LabelKeys.INFRA_UUID, uuids).delete();
        }
    }

    @Override
    public Optional<Secret> getSecret(String secretName) {
        return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(secretName).get());
    }

    private JsonObject doRawHttpRequest(String path, String method, JsonObject body, boolean errorOk) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve(path);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + controllerToken)
                .method(method, body != null ? RequestBody.create(MediaType.parse("application/json"), body.encode()) : null);

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            try (ResponseBody responseBody = response.body()) {
                String responseString = responseBody != null ? responseBody.string() : "{}";
                if (response.isSuccessful()) {
                    return new JsonObject(responseString);
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createRoleBinding(String name, String namespace, String refKind, String refName, List<Subject> subjectList) {

        Boolean isOpenShift = client.isAdaptable(OpenShiftClient.class);
        String apiVersion = isOpenShift ? "v1" : "rbac.authorization.k8s.io/v1";
        String apiPath = isOpenShift ? "/oapi/v1" : "/apis/rbac.authorization.k8s.io/v1";

        JsonObject body = new JsonObject();

        body.put("kind", "RoleBinding");
        body.put("apiVersion", apiVersion);

        JsonObject metadata = new JsonObject();
        metadata.put("name", name);
        metadata.put("namespace", namespace);
        body.put("metadata", metadata);

        JsonObject roleRef = new JsonObject();
        roleRef.put("apiGroup", "rbac.authorization.k8s.io");
        roleRef.put("kind", refKind);
        roleRef.put("name", refName);
        body.put("roleRef", roleRef);

        JsonArray subjects = new JsonArray();

        for (Subject subjectEntry : subjectList) {
            JsonObject subject = new JsonObject();
            if (isOpenShift) {
                subject.put("apiGroup", "rbac.authorization.k8s.io");
            }
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

    @Override
    public void createServiceAccount(String saName, Map<String, String> labels) {
        if (client.serviceAccounts().inNamespace(namespace).withName(saName).get() == null) {
            client.serviceAccounts().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(saName)
                    .withLabels(labels)
                    .endMetadata()
                    .done();
            createRoleBinding(saName + "-admin", namespace, "ClusterRole", "admin", Arrays.asList(new Subject("ServiceAccount", saName, namespace)));
        }
    }

    private static boolean isReady(Deployment deployment) {
        Integer unavailableReplicas = deployment.getStatus().getUnavailableReplicas();
        return unavailableReplicas == null || unavailableReplicas == 0;
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
