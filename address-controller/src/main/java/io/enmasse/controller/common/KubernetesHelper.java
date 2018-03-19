/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.authentication.TokenReviewBuilder;
import io.fabric8.kubernetes.api.model.authentication.TokenReviewStatus;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.kubernetes.client.utils.ImpersonatorInterceptor;
import io.fabric8.openshift.api.model.*;
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
    private static final String TEMPLATE_SUFFIX = ".json";

    private final NamespacedOpenShiftClient client;
    private final String namespace;
    private final String controllerToken;
    private final String environment;
    private final Optional<File> templateDir;
    private final String addressControllerSa;
    private final String addressSpaceAdminSa;
    private final boolean enableRbac;
    private final String impersonateUser;

    public KubernetesHelper(String namespace, NamespacedOpenShiftClient client, String token, String environment, Optional<File> templateDir, String addressControllerSa, String addressSpaceAdminSa, boolean enableRbac, String impersonateUser) {
        this.client = client;
        this.namespace = namespace;
        this.controllerToken = token;
        this.environment = environment;
        this.templateDir = templateDir;
        this.addressControllerSa = addressControllerSa;
        this.addressSpaceAdminSa = addressSpaceAdminSa;
        this.enableRbac = enableRbac;
        this.impersonateUser = impersonateUser;
    }

    @Override
    public void create(KubernetesList resources, String namespace, String impersonateUser) {
        try {
            ImpersonatorInterceptor.setImpersonateUser(impersonateUser);
            client.withRequestConfig(new RequestConfigBuilder()
                    .withImpersonateUsername(impersonateUser)
                    .build()).call(c -> c.lists().inNamespace(namespace).create(resources));
        } finally {
            ImpersonatorInterceptor.setImpersonateUser(null);
        }
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void delete(KubernetesList resources, String impersonateUser) {
        ImpersonatorInterceptor.setImpersonateUser(impersonateUser);
        try {
            client.withRequestConfig(new RequestConfigBuilder()
                    .withImpersonateUsername(impersonateUser)
                    .build()).call(c -> c.lists().inNamespace(namespace).delete(resources));
        } finally {
            ImpersonatorInterceptor.setImpersonateUser(null);
        }
    }

    @Override
    public void createNamespace(AddressSpace addressSpace) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            ImpersonatorInterceptor.setImpersonateUser(addressSpace.getCreatedBy());
            try {
                client.withRequestConfig(new RequestConfigBuilder()
                        .withImpersonateUsername(addressSpace.getCreatedBy())
                        .build()).call(c -> c.projectrequests().createNew()
                        .editOrNewMetadata()
                        .withName(addressSpace.getNamespace())
                        .endMetadata()
                        .done());

                client.configMaps().inNamespace(namespace).createNew()
                        .editOrNewMetadata()
                        .withName(addressSpace.getUid())
                        .addToLabels("app", "enmasse")
                        .addToLabels(LabelKeys.TYPE, "namespace")
                        .addToLabels(LabelKeys.ENVIRONMENT, environment)
                        .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                        .addToAnnotations(AnnotationKeys.NAMESPACE, addressSpace.getNamespace())
                        .addToAnnotations(AnnotationKeys.CREATED_BY, addressSpace.getCreatedBy())
                        .endMetadata()
                        .done();
            } finally {
                ImpersonatorInterceptor.setImpersonateUser(null);
            }

        } else {
            client.namespaces().createNew()
                    .editOrNewMetadata()
                    .withName(addressSpace.getNamespace())
                    .addToLabels("app", "enmasse")
                    .addToLabels(LabelKeys.TYPE, "namespace")
                    .addToLabels(LabelKeys.ENVIRONMENT, environment)
                    .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpace.getName())
                    .addToAnnotations(AnnotationKeys.UUID, addressSpace.getUid())
                    .endMetadata()
                    .done();
        }
    }

    @Override
    public Kubernetes withNamespace(String namespace) {
        return new KubernetesHelper(namespace, client, controllerToken, environment, templateDir, addressControllerSa, addressSpaceAdminSa, enableRbac, impersonateUser);
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
    public void addSystemImagePullerPolicy(String namespace, AddressSpace addressSpace) {
        if (isRBACSupported()) {
            try {
                ImpersonatorInterceptor.setImpersonateUser(impersonateUser);
                String groupName = "system:serviceaccounts:" + addressSpace.getNamespace();
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
            } finally {
                ImpersonatorInterceptor.setImpersonateUser(null);
            }
        }
    }

    @Override
    public void deleteNamespace(NamespaceInfo namespaceInfo) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            client.configMaps().inNamespace(namespace).withName(namespaceInfo.getConfigName()).delete();
            client.withRequestConfig(new RequestConfigBuilder()
                    .withImpersonateUsername(namespaceInfo.getCreatedBy())
                    .build()).call(c -> c.inNamespace(namespace).projects().withName(namespaceInfo.getNamespace()).delete());
        } else {
            client.namespaces().withName(namespaceInfo.getNamespace()).delete();
        }
    }

    @Override
    public boolean existsNamespace(String namespace, String impersonateUser) {
        if (client.isAdaptable(OpenShiftClient.class)) {
            try {
                ImpersonatorInterceptor.setImpersonateUser(impersonateUser);
                return client.projects().list().getItems().stream()
                        .anyMatch(p -> p.getMetadata().getName().equals(namespace));
            } finally {
                ImpersonatorInterceptor.setImpersonateUser(null);
            }
        } else {
            return client.namespaces().withName(namespace).get() != null;
        }
    }

    @Override
    public boolean hasService(String service) {
        return client.services().inNamespace(namespace).withName(service).get() != null;
    }

    @Override
    public HasMetadata createEndpoint(Endpoint endpoint, Service service, String addressSpaceName, String namespace) {
        if (service == null || service.getMetadata().getAnnotations() == null) {
            log.info("Skipping creating endpoint for unknown service {}", endpoint.getService());
            return null;
        }

        String defaultPort = service.getMetadata().getAnnotations().get(AnnotationKeys.ENDPOINT_PORT);

        if (client.isAdaptable(OpenShiftClient.class)) {
            RouteBuilder route = new RouteBuilder()
                    .editOrNewMetadata()
                    .withName(endpoint.getName())
                    .withNamespace(namespace)
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

            if (endpoint.getCertSpec().isPresent()) {
                route.editOrNewMetadata()
                        .addToAnnotations(AnnotationKeys.CERT_PROVIDER, endpoint.getCertSpec().get().getProvider())
                        .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, endpoint.getCertSpec().get().getSecretName())
                        .endMetadata()
                        .editOrNewSpec()
                        .withNewTls()
                        .withTermination("passthrough")
                        .endTls()
                        .endSpec();
            }
            return route.build();
        } else {
            if (service.getSpec().getPorts().isEmpty()) {
                return null;
            }
            ServicePort servicePort = null;
            for (ServicePort port : service.getSpec().getPorts()) {
                if (port.getName().equals(defaultPort)) {
                    servicePort = port;
                    break;
                }
            }
            if (servicePort != null) {
                ServiceBuilder svc = new ServiceBuilder()
                        .editOrNewMetadata()
                        .withName(endpoint.getName() + "-external")
                        .withNamespace(namespace)
                        .addToAnnotations(AnnotationKeys.ADDRESS_SPACE, addressSpaceName)
                        .addToAnnotations(AnnotationKeys.SERVICE_NAME, service.getMetadata().getName())
                        .addToLabels(LabelKeys.TYPE, "loadbalancer")
                        .endMetadata()
                        .editOrNewSpec()
                        .withPorts(servicePort)
                        .withSelector(service.getSpec().getSelector())
                        .withType("LoadBalancer")
                        .endSpec();
                if (endpoint.getCertSpec().isPresent()) {
                    svc.editOrNewMetadata()
                            .addToAnnotations(AnnotationKeys.CERT_PROVIDER, endpoint.getCertSpec().get().getProvider())
                            .addToAnnotations(AnnotationKeys.CERT_SECRET_NAME, endpoint.getCertSpec().get().getSecretName())
                            .endMetadata();
                }
                return svc.build();
            }
        }
        return null;
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
    public Set<NamespaceInfo> listAddressSpaces() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.APP, "enmasse");
        labels.put(LabelKeys.TYPE, "namespace");
        labels.put(LabelKeys.ENVIRONMENT, environment);
        if (client.isAdaptable(OpenShiftClient.class)) {
            return client.configMaps().inNamespace(namespace).withLabels(labels).list().getItems().stream()
                    .map(n -> new NamespaceInfo(n.getMetadata().getName(),
                            n.getMetadata().getAnnotations().get(AnnotationKeys.ADDRESS_SPACE),
                            n.getMetadata().getAnnotations().get(AnnotationKeys.NAMESPACE),
                            n.getMetadata().getAnnotations().get(AnnotationKeys.CREATED_BY)))
                    .collect(Collectors.toSet());
        } else {
            return client.namespaces().withLabels(labels).list().getItems().stream()
                    .map(n -> new NamespaceInfo(
                            n.getMetadata().getAnnotations().get(AnnotationKeys.UUID),
                            n.getMetadata().getAnnotations().get(AnnotationKeys.ADDRESS_SPACE),
                            n.getMetadata().getName(),
                            n.getMetadata().getAnnotations().get(AnnotationKeys.CREATED_BY)))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Optional<Secret> getSecret(String secretName) {
        return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(secretName).get());
    }

    private JsonObject doRawHttpRequest(String path, String method, JsonObject body, boolean errorOk, String impersonateUser) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve(path);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + controllerToken)
                .method(method, body != null ? RequestBody.create(MediaType.parse("application/json"), body.encode()) : null);

        if (impersonateUser != null) {
            requestBuilder.addHeader("Impersonate-User", impersonateUser);
        }

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


    @Override
    public TokenReview performTokenReview(String token) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "TokenReview");
            body.put("apiVersion", "authentication.k8s.io/v1beta1");

            JsonObject spec = new JsonObject();
            spec.put("token", token);
            body.put("spec", spec);

            JsonObject responseBody= doRawHttpRequest("/apis/authentication.k8s.io/v1beta1/tokenreviews", "POST", body, false, null);
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
    public SubjectAccessReview performSubjectAccessReview(String user, String namespace, String verb, String impersonateUser) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "LocalSubjectAccessReview");
            body.put("apiVersion", "authorization.k8s.io/v1beta1");

            JsonObject metadata = new JsonObject();
            metadata.put("namespace", namespace);
            body.put("metadata", metadata);

            JsonObject spec = new JsonObject();

            JsonObject resourceAttributes = new JsonObject();
            resourceAttributes.put("namespace", namespace);
            resourceAttributes.put("resource", "configmaps");
            resourceAttributes.put("verb", verb);

            spec.put("resourceAttributes", resourceAttributes);
            spec.put("user", user);

            body.put("spec", spec);
            JsonObject responseBody = doRawHttpRequest("/apis/authorization.k8s.io/v1beta1/namespaces/" + namespace + "/localsubjectaccessreviews", "POST", body, false, impersonateUser);

            JsonObject status = responseBody.getJsonObject("status");
            boolean allowed = false;
            if (status != null) {
                Boolean allowedMaybe = status.getBoolean("allowed");
                allowed = allowedMaybe == null ? false : allowedMaybe;
            }
            return new SubjectAccessReview(user, allowed);
        } else {
            return new SubjectAccessReview(user, false);
        }
    }

    @Override
    public boolean isRBACSupported() {
        return enableRbac &&
                client.supportsApiPath("/apis/authentication.k8s.io") &&
                client.supportsApiPath("/apis/authorization.k8s.io") &&
                client.supportsApiPath("/apis/rbac.authorization.k8s.io");

    }

    private void createRoleBinding(String name, String namespace, String refKind, String refName, List<Subject> subjectList, String impersonateUser) {

        String apiVersion = client.isAdaptable(OpenShiftClient.class) ? "v1" : "rbac.authorization.k8s.io/v1beta1";
        String apiPath = client.isAdaptable(OpenShiftClient.class) ? "/oapi/v1" : "/apis/rbac.authorization.k8s.io/v1beta1";

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
            subject.put("apiGroup", "rbac.authorization.k8s.io");
            subject.put("kind", subjectEntry.getKind());
            subject.put("name", subjectEntry.getName());
            if (subjectEntry.getNamespace() != null) {
                subject.put("namespace", subjectEntry.getNamespace());
            }
            subjects.add(subject);
        }

        body.put("subjects", subjects);


        doRawHttpRequest(apiPath + "/namespaces/" + namespace + "/rolebindings", "POST", body, false, impersonateUser);
    }

    @Override
    public void addAddressSpaceAdminRoleBinding(AddressSpace addressSpace) {
        if (isRBACSupported()) {
            createRoleBinding("addressspace-admins", addressSpace.getNamespace(), "ClusterRole", "admin", Arrays.asList(
                    new Subject("ServiceAccount", addressControllerSa, namespace)),
                    addressSpace.getCreatedBy());
        }
    }

    @Override
    public String getAddressSpaceAdminSa() {
        return addressSpaceAdminSa;
    }

    @Override
    public void createServiceAccount(String namespace, String saName, String impersonateUser) {
        ImpersonatorInterceptor.setImpersonateUser(impersonateUser);
        try {
            if (client.serviceAccounts().inNamespace(namespace).withName(saName).get() == null) {
                client.serviceAccounts().inNamespace(namespace).createNew()
                        .editOrNewMetadata()
                        .withName(saName)
                        .endMetadata()
                        .done();
            }
        } finally {
            ImpersonatorInterceptor.setImpersonateUser(null);
        }
    }

    @Override
    public void addAddressSpaceRoleBindings(AddressSpace addressSpace) {
        String namespace = addressSpace.getNamespace();

        if (isRBACSupported()) {
            createRoleBinding("address-space-viewers", namespace, "ClusterRole", "view", Arrays.asList(
                    new Subject("ServiceAccount", "default", namespace)), addressSpace.getCreatedBy());;
            createRoleBinding("address-admins", namespace, "ClusterRole", "edit", Arrays.asList(
                    new Subject("ServiceAccount", addressSpaceAdminSa, namespace)), addressSpace.getCreatedBy());
            if (hasClusterRole("event-reporter")) {
                try {
                    createRoleBinding("event-reporters", namespace, "ClusterRole", "event-reporter", Arrays.asList(
                            new Subject("ServiceAccount", addressSpaceAdminSa, namespace)), addressSpace.getCreatedBy());
                } catch (Exception e) {
                    log.warn("Unable to grant event create privileges");
                }
            }
        } else {
            log.info("No support for RBAC, won't add to address-admin role");
        }
    }

    private boolean hasClusterRole(String roleName) {
        String apiPath = client.isAdaptable(OpenShiftClient.class) ? "/oapi/v1" : "/apis/rbac.authorization.k8s.io/v1beta1";
        return doRawHttpRequest(apiPath + "/clusterroles/" + roleName, "GET", null, true, null) != null;
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
