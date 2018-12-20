/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.json.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;

import java.io.*;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class K8sServiceAccountCredentialProvider implements CredentialProvider, CredentialInputValidator, OnUserCache, CredentialInputUpdater {

    private static final Logger LOG = Logger.getLogger(K8sServiceAccountCredentialProvider.class);

    public static final String ENMASSE_SERVICE_ACCOUNT_TYPE = "enmasse-service-account";

    private final NamespacedOpenShiftClient client;
    private final OkHttpClient httpClient;

    K8sServiceAccountCredentialProvider(KeycloakSession session) {
        client = new DefaultOpenShiftClient();
        httpClient = client.adapt(OkHttpClient.class);

    }

    @Override
    public boolean supportsCredentialType(String s) {
        return ENMASSE_SERVICE_ACCOUNT_TYPE.equals(s);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String s) {
        return "serviceaccount".equals(userModel.getFirstAttribute("authenticationType")) ;
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        String token = ((UserCredentialModel) credentialInput).getValue();
        String userName = authenticateToken(token, client, httpClient);

        boolean authenticated = Objects.equals(userModel.getUsername(), userName);
        if(userName == null) {
            LOG.debug("User: " + userModel.getUsername() + " not authenticated for realm " + realmModel.getName());
        } else if(!authenticated) {
            LOG.debug("Attempt to log in for user " + userModel.getUsername() + " in realm " + realmModel.getName() + " with token for " + userName);
        }

        return authenticated;
    }

    public static String authenticateToken(String token, NamespacedOpenShiftClient client, OkHttpClient httpClient) {
        JsonObject body = new JsonObject();
        String userName = null;
        body.put("kind", "TokenReview");
        body.put("apiVersion", "authentication.k8s.io/v1beta1");

        JsonObject spec = new JsonObject();
        spec.put("token", token);
        body.put("spec", spec);

        JsonObject result;

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve("/apis/authentication.k8s.io/v1beta1/tokenreviews");
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + client.getConfiguration().getOauthToken())
                .method("POST", RequestBody.create(MediaType.parse("application/json"), body.encode()));

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            try (ResponseBody responseBody = response.body()) {
                String responseString = responseBody != null ? responseBody.string() : "{}";
                if (response.isSuccessful()) {
                    result = new JsonObject(responseString);
                } else {
                    String errorMessage = String.format("Error performing POST on /apis/authentication.k8s.io/v1beta1/tokenreviews: %d, %s", response.code(), responseString);
                    throw new RuntimeException(errorMessage);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        JsonObject status = result.getJsonObject("status");

        if (status != null) {
            Boolean auth = status.getBoolean("authenticated");
            if(auth == null ? false : auth) {
                JsonObject user = status.getJsonObject("user");
                if (user != null) {
                    userName = user.getString("username");
                }
            } else {
                LOG.debug("Token was not authenticated");
            }
        }
        return userName;
    }

    @Override
    public void onCache(RealmModel realmModel, CachedUserModel cachedUserModel, UserModel userModel) {
    }

    @Override
    public boolean updateCredential(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realmModel, UserModel userModel, String s) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realmModel, UserModel userModel) {
        if(!"serviceaccount".equals(userModel.getFirstAttribute("authenticationType"))) {
            return Collections.singleton(ENMASSE_SERVICE_ACCOUNT_TYPE);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
