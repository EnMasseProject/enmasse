/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.OAuthClientBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Random;


public class KeycloakController {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(KeycloakController.class);

    public static void main(String [] args) throws Exception {

        Map<String, String> env = System.getenv();

        NamespacedOpenShiftClient client = new DefaultOpenShiftClient();

        boolean autoCreate = Optional.ofNullable(System.getenv("AUTO_CREATE")).map(Boolean::parseBoolean).orElse(false);

        boolean isOpenShift = isOpenShift(client);

        if (autoCreate) {
            ensureConfigurationExists(client, env, isOpenShift);
            ensurePersistentVolumeClaimExists(client, env);
        }
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
            existingCredentials = client.secrets().createOrReplace(secretBuilder.build());
        } else {
            log.info("{} already exists, not generating", getKeycloakCredentialsSecretName(env));
        }

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
                            " Ensure that settings within the 'standard' AuthenticationService match those of the oauthclient" +
                            " supplied by the cluster's admin. {}";
                    if (log.isDebugEnabled()) {
                        log.debug(message, oauthClientName, e.getMessage(), e);
                    } else {
                        log.warn(message, oauthClientName, e.getMessage());
                    }
                } else {
                    throw e;
                }
            }
        }

        AuthenticationService existingServiceRef = client.customResources(AdminCrd.authenticationServices(), AuthenticationService.class, AuthenticationServiceList.class, DoneableAuthenticationService.class).inNamespace(client.getNamespace()).withName("standard").get();

        if (existingServiceRef == null) {

            AuthenticationServiceBuilder builder = new AuthenticationServiceBuilder()
                    .withNewMetadata()
                    .withName("standard")
                    .addToLabels(LabelKeys.APP, "enmasse")
                    .endMetadata()
                    .withNewSpec()
                    .withHost("standard-authservice")
                    .withPort(5671)
                    .withCaCertSecretName(getKeycloakCertSecretName(env))
                    .endSpec();

            if (isOpenShift) {
                String keycloakOauthUrl = getKeycloakAuthUrl(client, env);
                builder.editMetadata().addToAnnotations(AnnotationKeys.OAUTH_URL, keycloakOauthUrl);
                                String openshiftOauthUrl = getOpenShiftOauthUrl(client);
                if (openshiftOauthUrl == null || openshiftOauthUrl.contains("https://localhost:8443") || openshiftOauthUrl.contains("https://127.0.0.1:8443")) {
                    openshiftOauthUrl = String.format("https://%s:%s", env.get("KUBERNETES_SERVICE_HOST"), env.get("KUBERNETES_SERVICE_PORT"));
                }
                builder.editMetadata().addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_URL, openshiftOauthUrl);
            }

            if (oauthClientAccessible) {
                builder.editMetadata().addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_ID, oauthClientName);
                builder.editMetadata().addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_SECRET, oauthClientSecret);
            } else {
                log.warn("Unable to initialize identityProviderClientId and identityProviderClientSecret " +
                        "on authentication service 'standard' as these details cannot be determined automatically.");
            }

            builder.editMetadata().addToAnnotations(AnnotationKeys.KEYCLOAK_CREDENTIALS_SECRET_NAME, existingCredentials.getMetadata().getName());

            AuthenticationService authenticationService = builder.build();
            client.customResources(AdminCrd.authenticationServices(), AuthenticationService.class, AuthenticationServiceList.class, DoneableAuthenticationService.class).inNamespace(client.getNamespace()).create(
                    authenticationService);
            log.debug("Created authentication service: {} ", authenticationService);
        } else if (oauthClientAccessible) {
            boolean updateRequired = false;
            log.info("{} already exists, not generating", existingServiceRef.getMetadata().getName());
            if (!oauthClientName.equals(existingServiceRef.getAnnotation(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_ID))) {
                existingServiceRef.putAnnotation(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_ID, oauthClientName);
                log.info("Updating identityProviderClientId: {}", oauthClientName);
                updateRequired = true;
            }
            if (!oauthClientSecret.equals(existingServiceRef.getAnnotation(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_SECRET))) {
                existingServiceRef.putAnnotation(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_SECRET, oauthClientSecret);
                log.info("Updating identityProviderClientSecret");
                updateRequired = true;
            }

            if (updateRequired) {
                client.customResources(AdminCrd.authenticationServices(), AuthenticationService.class, AuthenticationServiceList.class, DoneableAuthenticationService.class).inNamespace(client.getNamespace()).createOrReplace(existingServiceRef);
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
