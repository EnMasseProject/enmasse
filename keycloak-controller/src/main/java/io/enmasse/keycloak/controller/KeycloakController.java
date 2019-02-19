/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

import io.fabric8.kubernetes.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.ResourceChecker;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.OAuthClientBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.User;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class KeycloakController {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(KeycloakController.class);

    public static void main(String [] args) throws Exception {

        Map<String, String> env = System.getenv();

        NamespacedOpenShiftClient client = new DefaultOpenShiftClient();
        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(client);

        boolean autoCreate = Optional.ofNullable(System.getenv("AUTO_CREATE")).map(Boolean::parseBoolean).orElse(false);

        boolean isOpenShift = isOpenShift(client);

        if (autoCreate) {
            ensureConfigurationExists(client, env, isOpenShift);
            ensurePersistentVolumeClaimExists(client, env);
        }

        final String keycloakConfigName = getKeycloakConfigName(env);
        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(client,
                keycloakConfigName,
                getKeycloakCredentialsSecretName(env),
                getKeycloakCertSecretName(env));

        KubeApi kubeApi = new KubeApi() {
            @Override
            public String findUserId(String userName) {
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
            }

            @Override
            public KeycloakRealmParams getIdentityProviderParams() {
                return KeycloakRealmParams.fromKube(client, keycloakConfigName);
            }
        };


        UserApi userApi = new KeycloakUserApi(keycloakFactory, Clock.systemUTC());

        KeycloakManager keycloakManager = new KeycloakManager(new Keycloak(keycloakFactory), kubeApi, userApi);

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

    private static void ensurePersistentVolumeClaimExists(NamespacedOpenShiftClient client, Map<String, String> env) {
        String pvcName = getKeycloakPvcName(env);
        String pvcStorageSize = getKeycloakPvcStorageSize(env);
        PersistentVolumeClaim pvc = client.persistentVolumeClaims().withName(pvcName).get();
        if (pvc == null) {
            client.persistentVolumeClaims().createNew()
                    .editOrNewMetadata()
                    .withName(pvcName)
                    .addToLabels("app", "enmasse")
                    .endMetadata()
                    .editOrNewSpec()
                    .addToAccessModes("ReadWriteOnce")
                    .editOrNewResources()
                    .addToRequests("storage", new Quantity(pvcStorageSize))
                    .endResources()
                    .endSpec()
                    .done();
        }
    }

    private static String getKeycloakPvcName(Map<String, String> env) {
        return getEnv(env, "KEYCLOAK_PVC_NAME").orElse("keycloak-data");
    }

    private static String getKeycloakPvcStorageSize(Map<String, String> env) {
        return getEnv(env, "KEYCLOAK_PVC_STORAGE_SIZE").orElse("5Gi");
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

    private static String getOauthClientName(Map<String, String> env, String namespace) {
        return getEnv(env, "OAUTH_CLIENT_NAME").orElse("enmasse-oauthclient-" + namespace);
    }

    private static String getOauthClientGrantMethod(Map<String, String> env) {
        return getEnv(env, "OAUTH_CLIENT_GRANT_METHOD").orElse("auto");
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
        PasswordGenerator passwordGenerator = new PasswordGenerator();

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

        final String keycloakConfigName = getKeycloakConfigName(env);
        final String oauthClientName = getOauthClientName(env, client.getNamespace());
        String oauthClientSecret = null;
        boolean oauthClientAccessible = false;

        if (isOpenShift) {
            try {
                OAuthClient oauthClient = client.oAuthClients().withName(oauthClientName).get();
                if (oauthClient == null) {
                    String keycloakOauthUrl = getKeycloakAuthUrl(client, env);
                    oauthClientSecret = passwordGenerator.generateAlphaNumberic(32);
                    String grantMethod = getOauthClientGrantMethod(env);
                    OAuthClient c = new OAuthClientBuilder()
                            .editOrNewMetadata()
                            .withName(oauthClientName)
                            .addToLabels("app", "enmasse")
                            .endMetadata()
                            .withGrantMethod(grantMethod)
                            .withApiVersion("v1")
                            .withKind("OAuthClient")
                            .withRedirectURIs(keycloakOauthUrl)
                            .withSecret(oauthClientSecret)
                            .build();

                    oauthClient = client.oAuthClients().createOrReplace(c);
                    log.info("Created OAuthClient: {} ", oauthClient.getMetadata().getName());
                } else {
                    oauthClientSecret = oauthClient.getSecret();
                }
                oauthClientAccessible = true;
            } catch (KubernetesClientException e) {
                if (String.valueOf(e.getMessage()).toLowerCase().contains("forbidden!")) {
                    String message = "Unable to get or create the oauthclient '{}' owing to cluster access restrictions." +
                            " Ensure that settings within the configmap '{}' match those of the oauthclient" +
                            " supplied by the cluster's admin. {}";
                    if (log.isDebugEnabled()) {
                        log.debug(message, oauthClientName, keycloakConfigName, e.getMessage(), e);
                    } else {
                        log.warn(message, oauthClientName, keycloakConfigName, e.getMessage());
                    }
                } else {
                    throw e;
                }
            }
        }

        ConfigMap existingConfig = client.configMaps().withName(keycloakConfigName).get();

        if (existingConfig == null) {
            ConfigMapBuilder configMapBuilder = new ConfigMapBuilder()
                    .editOrNewMetadata()
                    .withName(keycloakConfigName)
                    .addToLabels("app", "enmasse")
                    .endMetadata();

            if (isOpenShift) {
                String keycloakOauthUrl = getKeycloakAuthUrl(client, env);
                configMapBuilder.addToData("oauthUrl", keycloakOauthUrl);

                String openshiftOauthUrl = getOpenShiftOauthUrl(client);
                if (openshiftOauthUrl == null || openshiftOauthUrl.contains("https://localhost:8443") || openshiftOauthUrl.contains("https://127.0.0.1:8443")) {
                    openshiftOauthUrl = String.format("https://%s:%s", env.get("KUBERNETES_SERVICE_HOST"), env.get("KUBERNETES_SERVICE_PORT"));
                }
                configMapBuilder.addToData("identityProviderUrl", openshiftOauthUrl);
            }

            if (oauthClientAccessible) {
                configMapBuilder.addToData("identityProviderClientId", oauthClientName);
                configMapBuilder.addToData("identityProviderClientSecret", oauthClientSecret);
            } else {
                log.warn("Unable to initialize identityProviderClientId and identityProviderClientSecret " +
                        "on config map {} as these details cannot be determined automatically.", keycloakConfigName);
            }

            configMapBuilder.addToData("hostname", "standard-authservice");
            configMapBuilder.addToData("port", "5671");
            configMapBuilder.addToData("caSecretName", getKeycloakCertSecretName(env));

            client.configMaps().createOrReplace(configMapBuilder.build());
            log.debug("Created config map: {} ", keycloakConfigName);
        } else if (oauthClientAccessible) {
            boolean updateRequired = false;
            log.info("{} already exists, not generating", keycloakConfigName);
            Map<String, String> updateMap = new HashMap<>(existingConfig.getData());
            if (!oauthClientName.equals(updateMap.get("identityProviderClientId"))) {
                updateMap.put("identityProviderClientId", oauthClientName);
                log.info("Updating identityProviderClientId: {}", oauthClientName);
                updateRequired = true;
            }
            if (!oauthClientSecret.equals(updateMap.get("identityProviderClientSecret"))) {
                updateMap.put("identityProviderClientSecret", oauthClientSecret);
                log.info("Updating identityProviderClientSecret");
                updateRequired = true;
            }

            if (updateRequired) {
                existingConfig.setData(updateMap);
                client.configMaps().createOrReplace(existingConfig);
            }
        }
    }

    private static String getKeycloakAuthUrl(NamespacedOpenShiftClient client, Map<String, String> env) {
		Route keycloakRoute = client.routes().withName("keycloak").get();
		String keycloakOauthUrl = null;
		if (keycloakRoute != null && !keycloakRoute.getSpec().getHost().contains("127.0.0.1")) {
            keycloakOauthUrl = String.format("https://%s/auth",  keycloakRoute.getSpec().getHost());
		} else {
		    keycloakOauthUrl = getEnv(env, "STANDARD_AUTHSERVICE_SERVICE_HOST").map(ip -> "https://" + ip + ":8443/auth").orElse(null);
		}
		return keycloakOauthUrl;
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
                    Map<?,?> values = mapper.readValue(responseString, Map.class);
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
