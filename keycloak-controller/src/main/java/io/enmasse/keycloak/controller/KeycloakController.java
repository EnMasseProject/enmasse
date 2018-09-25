/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.ResourceChecker;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.User;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import okhttp3.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class KeycloakController {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(KeycloakController.class);

    public static void main(String [] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Map<String, String> env = System.getenv();

        NamespacedOpenShiftClient client = new DefaultOpenShiftClient();
        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(client);

        boolean autoCreate = Optional.ofNullable(System.getenv("AUTO_CREATE")).map(Boolean::parseBoolean).orElse(false);

        boolean isOpenShift = isOpenShift(client);

        if (autoCreate) {
            ensureConfigurationExists(client, env, isOpenShift);
        }

        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(client,
                getKeycloakConfigName(env),
                getKeycloakCredentialsSecretName(env),
                getKeycloakCertSecretName(env));

        IdentityProviderParams identityProviderParams = IdentityProviderParams.fromKube(client, getKeycloakConfigName(env));

        log.info("Started with identity provider params: {}", identityProviderParams);

        KubeApi kubeApi = userName -> {
            if (isOpenShift) {
                if (userName == null || userName.isEmpty() || userName.contains(":")) {
                    return "";
                }
                try {
                    User user = client.users().withName(userName).get();
                    if (user == null) {
                        return "";
                    }
                    log.info("Found user {} with id {}", user.getMetadata().getName(), user.getMetadata().getUid());
                    return user.getMetadata().getUid();
                } catch (KubernetesClientException e) {
                    log.warn("Exception looking up user id, returning empty", e);
                    return "";
                }
            } else {
                return "";
            }
        };


        UserApi userApi = new KeycloakUserApi(keycloakFactory, Clock.systemUTC());

        KeycloakManager keycloakManager = new KeycloakManager(new Keycloak(keycloakFactory, identityProviderParams), kubeApi, userApi);

        Duration resyncInterval = getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5));

        Duration checkInterval = getEnv(env, "CHECK_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30));

        ResourceChecker<AddressSpace> resourceChecker = new ResourceChecker<>(keycloakManager, checkInterval);
        resourceChecker.start();
        addressSpaceApi.watchAddressSpaces(resourceChecker, resyncInterval);
    }

    private static String getKeycloakRouteName(Map<String, String> env) {
        return getEnv(env, "KEYCLOAK_ROUTE_NAME").orElse("keycloak");
    }

    private static String getKeycloakConfigName(Map<String, String> env) {
        return getEnv(env, "KEYCLOAK_CONFIG_NAME").orElse("keycloak-config");
    }

    private static String getKeycloakCredentialsSecretName(Map<String, String> env) {
        return getEnv(env, "KEYCLOAK_CREDENTIALS_SECRET_NAME").orElse("keycloak-credentials");
    }

    private static String getKeycloakCertSecretName(Map<String, String> env) {
        return getEnv(env, "KEYCLOAK_CERT_SECRET_NAME").orElse("standard-authservice-cert");
    }

    private static boolean isOpenShift(NamespacedOpenShiftClient client) {
        // Need to query the full API path because Kubernetes does not allow GET on /
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);
        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve("/apis/user.openshift.io");
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            return response.code() >= 200 && response.code() < 300;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void ensureConfigurationExists(NamespacedOpenShiftClient client, Map<String, String> env, boolean isOpenShift) {
        if (isOpenShift) {
            Route keycloakRoute = client.routes().withName(getKeycloakRouteName(env)).get();
            if (keycloakRoute == null) {
                Secret certSecret = client.secrets().withName(getKeycloakCertSecretName(env)).get();
                if (certSecret != null) {
                    String caCertificate = new String(Base64.getDecoder().decode(certSecret.getData().get("tls.crt")), StandardCharsets.UTF_8);
                    client.routes().createNew()
                            .editOrNewMetadata()
                            .withName(getKeycloakRouteName(env))
                            .addToLabels("app", "enmasse")
                            .endMetadata()
                            .editOrNewSpec()
                            .editOrNewPort()
                            .withNewTargetPort("https")
                            .endPort()
                            .editOrNewTo()
                            .withKind("Service")
                            .withName("standard-authservice")
                            .endTo()
                            .editOrNewTls()
                            .withTermination("reencrypt")
                            .withCaCertificate(caCertificate)
                            .endTls()
                            .endSpec()
                            .done();
                }
            }
        }

        Secret existingCredentials = client.secrets().withName(getKeycloakCredentialsSecretName(env)).get();
        if (existingCredentials == null) {
            PasswordGenerator passwordGenerator = new PasswordGenerator();
            byte [] adminUser = "admin".getBytes(StandardCharsets.UTF_8);
            byte [] adminPassword = passwordGenerator.generateAlphaNumberic(32).getBytes(StandardCharsets.UTF_8);
            Base64.Encoder b64enc = Base64.getEncoder();
            SecretBuilder secretBuilder = new SecretBuilder()
                    .editOrNewMetadata()
                    .withName(getKeycloakCredentialsSecretName(env))
                    .addToLabels("app", "enmasse")
                    .endMetadata()
                    .addToData("admin.username", b64enc.encodeToString(adminUser))
                    .addToData("admin.password", b64enc.encodeToString(adminPassword))
                    .withType("Opaque");
            client.secrets().createOrReplace(secretBuilder.build());
        } else {
            log.info("{} already exists, not generating", getKeycloakCredentialsSecretName(env));
        }

        ConfigMap existingConfig = client.configMaps().withName(getKeycloakConfigName(env)).get();
        if (existingConfig == null) {
            ConfigMapBuilder configMapBuilder = new ConfigMapBuilder()
                    .editOrNewMetadata()
                    .withName(getKeycloakConfigName(env))
                    .addToLabels("app", "enmasse")
                    .endMetadata();

            if (isOpenShift) {
                Route keycloakRoute = client.routes().withName("keycloak").get();
                String keycloakOauthUrl = null;
                if (keycloakRoute != null && !keycloakRoute.getSpec().getHost().contains("127.0.0.1")) {
                    keycloakOauthUrl = String.format("https://%s/auth",  keycloakRoute.getSpec().getHost());
                } else {
                    keycloakOauthUrl = getEnv(env, "STANDARD_AUTHSERVICE_SERVICE_HOST").map(ip -> "https://" + ip + ":8443/auth").orElse(null);
                }
                configMapBuilder.addToData("oauthUrl", keycloakOauthUrl);

                String openshiftOauthUrl = getOpenShiftOauthUrl(client);
                if (openshiftOauthUrl == null || openshiftOauthUrl.contains("https://localhost:8443") || openshiftOauthUrl.contains("https://127.0.0.1:8443")) {
                    openshiftOauthUrl = String.format("https://%s:%s", env.get("KUBERNETES_SERVICE_HOST"), env.get("KUBERNETES_SERVICE_PORT"));
                }
                configMapBuilder.addToData("identityProviderUrl", openshiftOauthUrl);
                String saName = "kc-oauth";
                ServiceAccount oauthClient = client.serviceAccounts().withName(saName).get();

                if (oauthClient != null) {

                    if (oauthClient.getMetadata().getAnnotations() == null || oauthClient.getMetadata().getAnnotations().get("serviceaccounts.openshift.io/oauth-redirecturi.first") == null) {
                        client.serviceAccounts().withName(saName).edit()
                                .editMetadata()
                                .addToAnnotations("serviceaccounts.openshift.io/oauth-redirecturi.first", keycloakOauthUrl)
                                .endMetadata()
                                .done();
                    }
                    configMapBuilder.addToData("identityProviderClientId", "system:serviceaccount:" + oauthClient.getMetadata().getNamespace() + ":" + oauthClient.getMetadata().getName());
                    for (ObjectReference secretRef : oauthClient.getSecrets()) {
                        Secret secret = client.secrets().withName(secretRef.getName()).get();
                        if (secret != null && secret.getData().containsKey("token")) {
                            configMapBuilder.addToData("identityProviderClientSecret", new String(Base64.getDecoder().decode(secret.getData().get("token")), StandardCharsets.UTF_8));
                            log.info("Located identity provider client secret");
                            break;
                        }
                    }
                }
            }

            configMapBuilder.addToData("hostname", "standard-authservice");
            configMapBuilder.addToData("port", "5671");
            configMapBuilder.addToData("caSecretName", getKeycloakCertSecretName(env));

            client.configMaps().createOrReplace(configMapBuilder.build());
        } else {
            log.info("{} already exists, not generating", getKeycloakConfigName(env));
        }
    }

    private static String getOpenShiftOauthUrl(NamespacedOpenShiftClient client) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve("/.well-known/oauth-authorization-server");
        log.info("Getting Oauth URL at {}", url.toString());
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            try (ResponseBody responseBody = response.body()) {
                String responseString = responseBody != null ? responseBody.string() : "{}";
                if (response.isSuccessful()) {
                    Map values = mapper.readValue(responseString, Map.class);
                    return (String) values.get("issuer");
                } else {
                    String errorMessage = String.format("Error retrieving OpenShift OAUTH URL: %d, %s", response.code(), responseString);
                    log.warn(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    public static class PasswordGenerator {
        private final Random random = new SecureRandom();
        private static final String ALPHA_NUMERIC_STRING = "abcdefghijklmnopqrstuvwxyz0123456789";

        private String generateAlphaNumberic(int length) {
            StringBuilder builder = new StringBuilder();
            while (length-- != 0) {
                int character = (int) (random.nextDouble() * ALPHA_NUMERIC_STRING.length());
                builder.append(ALPHA_NUMERIC_STRING.charAt(character));
            }
            return builder.toString();
        }
    }
}
