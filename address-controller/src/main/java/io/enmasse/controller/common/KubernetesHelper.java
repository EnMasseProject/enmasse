/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.controller.common;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DoneableIngress;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.DoneableRoleBinding;
import io.fabric8.openshift.api.model.DoneableRoute;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.api.model.RoleBindingBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
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
    private static final String TEMPLATE_SUFFIX = ".json";

    private final OpenShiftClient client;
    private final String namespace;
    private final String controllerToken;
    private final Optional<File> templateDir;

    public KubernetesHelper(String namespace, OpenShiftClient client, String token, Optional<File> templateDir) {
        this.client = client;
        this.namespace = namespace;
        this.controllerToken = token;
        this.templateDir = templateDir;
    }

    @Override
    public List<AddressCluster> listClusters() {
        Map<String, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.extensions().deployments().inNamespace(namespace).list().getItems());
        objects.addAll(client.persistentVolumeClaims().inNamespace(namespace).list().getItems());
        objects.addAll(client.configMaps().inNamespace(namespace).list().getItems());
        objects.addAll(client.replicationControllers().inNamespace(namespace).list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> annotations = config.getMetadata().getAnnotations();

            if (annotations != null && annotations.containsKey(AnnotationKeys.CLUSTER_ID)) {
                String groupId = annotations.get(AnnotationKeys.CLUSTER_ID);

                Map<String, String> labels = config.getMetadata().getLabels();

                if (labels != null && !"address-config".equals(labels.get(LabelKeys.TYPE))) {
                    if (!resourceMap.containsKey(groupId)) {
                        resourceMap.put(groupId, new ArrayList<>());
                    }
                    resourceMap.get(groupId).add(config);
                }
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> {
                    KubernetesList list = new KubernetesList();
                    list.setItems(entry.getValue());
                    return new AddressCluster(entry.getKey(), list);
                }).collect(Collectors.toList());
    }

    @Override
    public void create(HasMetadata... resources) {
        create(new KubernetesListBuilder()
                .addToItems(resources)
                .build());
    }

    @Override
    public void create(KubernetesList resources) {
        client.lists().inNamespace(namespace).create(resources);
    }

    @Override
    public void create(KubernetesList resources, String namespace) {
        client.lists().inNamespace(namespace).create(resources);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void delete(KubernetesList resources) {
        client.lists().inNamespace(namespace).delete(resources);
    }

    @Override
    public void delete(HasMetadata... resources) {
        delete(new KubernetesListBuilder()
                .addToItems(resources)
                .build());
    }

    @Override
    public Namespace createNamespace(String name, String namespace) {
        return client.namespaces().createNew()
                .editOrNewMetadata()
                    .withName(namespace)
                    .addToLabels("app", "enmasse")
                    .addToLabels(LabelKeys.TYPE, "address-space")
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, name)
                .endMetadata()
                .done();
    }

    @Override
    public Kubernetes withNamespace(String namespace) {
        return new KubernetesHelper(namespace, client, controllerToken, templateDir);
    }

    @Override
    public KubernetesList processTemplate(String templateName, ParameterValue... parameterValues) {
        if (templateDir.isPresent()) {
            File templateFile = new File(templateDir.get(), templateName + TEMPLATE_SUFFIX);
            return client.templates().load(templateFile).processLocally(parameterValues);
        } else {
            return client.templates().withName(templateName).process(parameterValues);
        }
    }

    @Override
    public void addSystemImagePullerPolicy(String namespace, String tenantNamespace) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            String groupName = "system:serviceaccounts:" + tenantNamespace;
            log.info("Adding system:image-pullers policy for {}", groupName);

            client.roleBindings()
                    .inNamespace(namespace)
                    .withName("system:image-pullers")
                    .edit()
                    .addToGroupNames(groupName)
                    .addNewSubject()
                    .withKind("SystemGroup")
                    .withName(groupName)
                    .endSubject()
                    .done();
        } else {
            // TODO: Add support for Kubernetes RBAC policies
            log.info("No support for Kubernetes RBAC policies yet, won't add to system:image-pullers");
        }
    }

    @Override
    public void deleteNamespace(String namespace) {
        client.namespaces().withName(namespace).delete();
    }

    @Override
    public boolean hasService(String service) {
        return client.services().inNamespace(namespace).withName(service).get() != null;
    }

    @Override
    public void createEndpoint(Endpoint endpoint, Service service, String addressSpaceName, String namespace) {
        if (service == null || service.getMetadata().getAnnotations() == null) {
            log.info("Skipping creating endpoint for unknown service {}", endpoint.getService());
            return;
        }

        String defaultPort = service.getMetadata().getAnnotations().get(AnnotationKeys.ENDPOINT_PORT);

        // TODO: Add labels
        if (client.isAdaptable(OpenShiftClient.class)) {
            DoneableRoute route = client.routes().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(endpoint.getName())
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                    .endMetadata()
                    .editOrNewSpec()
                    .withHost(endpoint.getHost().orElse(""))
                    .withNewTo()
                    .withName(endpoint.getService())
                    .withKind("Service")
                    .endTo()
                    .withNewPort()
                    .editOrNewTargetPort()
                    .withStrVal(defaultPort)
                    .endTargetPort()
                    .endPort()
                    .endSpec();

            if (endpoint.getCertProvider().isPresent()) {
                route.editOrNewMetadata()
                        .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, endpoint.getCertProvider().get().getSecretName())
                        .endMetadata()
                        .editOrNewSpec()
                        .withNewTls()
                        .withTermination("passthrough")
                        .endTls()
                        .endSpec();
            }
            route.done();
        } else {
            DoneableIngress ingress = client.extensions().ingresses().inNamespace(namespace).createNew()
                    .editOrNewMetadata()
                    .withName(endpoint.getName())
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                    .endMetadata()
                    .editOrNewSpec()
                    .editOrNewBackend()
                    .withServiceName(endpoint.getService())
                    .withServicePort(new IntOrString(defaultPort))
                    .endBackend()
                    .endSpec();

            if (endpoint.getCertProvider().isPresent()) {
                ingress.editOrNewMetadata()
                        .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, endpoint.getCertProvider().get().getSecretName())
                        .endMetadata()
                        .editOrNewSpec();

                if (endpoint.getHost().isPresent()) {
                    ingress.editOrNewSpec()
                            .addNewTl()
                            .addToHosts(endpoint.getHost().get())
                            .withSecretName(endpoint.getCertProvider().get().getSecretName())
                            .endTl();
                }
            }
            ingress.done();
        }
    }

    public Set<Deployment> getReadyDeployments() {
        return client.extensions().deployments().inNamespace(namespace).list().getItems().stream()
                .filter(KubernetesHelper::isReady)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isDestinationClusterReady(String clusterId) {
        return listClusters().stream()
                .filter(dc -> clusterId.equals(dc.getClusterId()))
                .anyMatch(KubernetesHelper::areAllDeploymentsReady);
    }

    @Override
    public List<Namespace> listNamespaces(Map<String, String> labels) {
        return client.namespaces().withLabels(labels).list().getItems();
    }

    @Override
    public List<Pod> listRouters() {
        return client.pods().withLabel(LabelKeys.CAPABILITY, "router").list().getItems();
    }

    @Override
    public Optional<Secret> getSecret(String secretName) {
        return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(secretName).get());
    }

    private JsonObject doRawHttpRequest(String path, String method, JsonObject body, boolean errorOk) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve(path);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + controllerToken)
                .method(method, body != null ? RequestBody.create(MediaType.parse("application/json"), body.encode()) : null)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return new JsonObject(response.body().string());
            } else {
                if (errorOk) {
                    response.close();
                    return null;
                } else {
                    String errorMessage = String.format("Error performing %s on %s: %d, %s", method, path, response.code(), response.body());
                    log.warn(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    public TokenReview performTokenReview(String token) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "TokenReview");
            body.put("apiVersion", "authentication.k8s.io/v1beta1");

            JsonObject spec = new JsonObject();
            spec.put("token", token);
            body.put("spec", spec);

            JsonObject responseBody= doRawHttpRequest("/apis/authentication.k8s.io/v1beta1/tokenreviews", "POST", body, false);
            JsonObject status = responseBody.getJsonObject("status");
            boolean authenticated = false;
            String userName = null;
            if (status != null) {
                Boolean auth = status.getBoolean("authenticated");
                authenticated = auth == null ? false : auth;
                JsonObject user = status.getJsonObject("user");
                if (user != null) {
                    userName = user.getString("username");
                }
            }
            return new TokenReview(userName, authenticated);
        } else {
            return new TokenReview(null, false);
        }
    }

    @Override
    public SubjectAccessReview performSubjectAccessReview(String user, String namespace, String verb) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "LocalSubjectAccessReview");
            body.put("apiVersion", "v1");

            body.put("namespace", namespace);
            body.put("resource", "configmaps");
            body.put("verb", verb);

            body.put("user", user);

            JsonObject responseBody = doRawHttpRequest("/oapi/v1/namespaces/" + this.namespace + "/localsubjectaccessreviews", "POST", body, false);
            Boolean allowed = responseBody.getBoolean("allowed");
            return new SubjectAccessReview(user, allowed == null ? false : allowed);
        } else {
            return new SubjectAccessReview(user, false);
        }
    }

    @Override
    public void addTenantAdminRole(String namespace) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            client.roles().inNamespace(namespace).createNew()
                    .withNewMetadata()
                    .withName("address-admin")
                    .endMetadata()
                    .addNewRule()
                    .addToResources("configmaps")
                    .addToVerbs("create", "list", "get", "delete", "watch", "update")
                    .endRule()
                    .done();

            String groupName = "system:serviceaccounts:" + namespace;
            Resource<RoleBinding, DoneableRoleBinding> bindingResource = client.roleBindings().inNamespace(namespace).withName("address-admin");
            RoleBinding roleBinding = new RoleBindingBuilder()
                    .editOrNewMetadata()
                    .withName("address-admin")
                    .withNamespace(namespace)
                    .endMetadata()
                    .addToGroupNames(groupName)
                    .addNewSubject()
                    .withKind("SystemGroup")
                    .withName(groupName)
                    .endSubject()
                    .withNewRoleRef()
                    .withName("address-admin")
                    .withNamespace(namespace)
                    .endRoleRef()
                    .build();
            bindingResource.create(roleBinding);
        } else {
            // TODO: Add support for Kubernetes RBAC policies
            log.info("No support for Kubernetes RBAC policies yet, won't add to address-admin role");
        }
    }

    public static boolean isDeployment(HasMetadata res) {
        return res.getKind().equals("Deployment");  // TODO: is there an existing constant for this somewhere?
    }

    private static boolean areAllDeploymentsReady(AddressCluster dc) {
        return dc.getResources().getItems().stream().filter(KubernetesHelper::isDeployment).allMatch(d -> isReady((Deployment) d));
    }

    private static boolean isReady(Deployment deployment) {
        Integer unavailableReplicas = deployment.getStatus().getUnavailableReplicas();
        return unavailableReplicas == null || unavailableReplicas == 0;
    }
}
