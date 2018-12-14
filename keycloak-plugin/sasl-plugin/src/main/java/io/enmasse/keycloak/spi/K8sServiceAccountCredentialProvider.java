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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class K8sServiceAccountCredentialProvider implements CredentialProvider, CredentialInputValidator, OnUserCache, CredentialInputUpdater {

    private static final Logger LOG = Logger.getLogger(AmqpServer.class);

    public static final String ENMASSE_SERVICE_ACCOUNT_TYPE = "enmasse-service-account";

    K8sServiceAccountCredentialProvider(KeycloakSession session) {
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
        JsonObject body = new JsonObject();

        body.put("kind", "TokenReview");
        body.put("apiVersion", "authentication.k8s.io/v1beta1");

        JsonObject spec = new JsonObject();
        spec.put("token", ((UserCredentialModel)credentialInput).getValue());
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
        if(!authenticated) {
            LOG.info("User: " + userModel.getUsername() + " not authenticated for realm " + realmModel.getName());
        } else if(!Objects.equals(userModel.getUsername(), userName)) {
            LOG.info("Attempt to log in for user " + userModel.getUsername() + " in realm " + realmModel.getName() + " with token for " + userName);
        }

        return authenticated && Objects.equals(userModel.getUsername(), userName);
    }

    private JsonObject doRawHttpRequest(String path, String method, JsonObject body, boolean errorOk) {
        NamespacedOpenShiftClient client = new DefaultOpenShiftClient();
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve(path);
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + client.getConfiguration().getOauthToken())
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
                        throw new RuntimeException(errorMessage);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

}
