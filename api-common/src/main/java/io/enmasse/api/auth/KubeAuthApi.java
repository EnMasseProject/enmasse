/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.api.model.User;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.json.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class KubeAuthApi implements AuthApi {
    private static final Logger log = LoggerFactory.getLogger(KubeAuthApi.class);
    private final NamespacedOpenShiftClient client;
    private final String impersonateUser;
    private final String apiToken;

    public KubeAuthApi(NamespacedOpenShiftClient client, String impersonateUser, String apiToken) {
        this.client = client;
        this.impersonateUser = impersonateUser;
        this.apiToken = apiToken;
    }

    private JsonObject doRawHttpRequest(String path, String method, JsonObject body, boolean errorOk, String impersonateUser) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve(path);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiToken)
                .method(method, body != null ? RequestBody.create(MediaType.parse("application/json"), body.encode()) : null);

        if (impersonateUser != null && !impersonateUser.isEmpty()) {
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
            String userId = null;
            if (status != null) {
                Boolean auth = status.getBoolean("authenticated");
                authenticated = auth == null ? false : auth;
                JsonObject user = status.getJsonObject("user");
                if (user != null) {
                    userName = user.getString("username");
                    userId = user.getString("uid");
                }
            }
            return new TokenReview(userName, userId, authenticated);
        } else {
            return new TokenReview(null, null, false);
        }
    }

    @Override
    public io.enmasse.api.auth.SubjectAccessReview performSubjectAccessReview(String user, String namespace, String verb) {
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
            return new io.enmasse.api.auth.SubjectAccessReview(user, allowed);
        } else {
            return new SubjectAccessReview(user, false);
        }
    }

    @Override
    public String getCert(String secretName, String namespace) {
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret == null) {
            throw new InternalServerErrorException("Cannot get secret " + secretName);
        }
        Map<String, String> caData = secret.getData();
        return new String(Base64.getDecoder().decode(caData.get("tls.crt")), StandardCharsets.UTF_8);
    }

    @Override
    public String getNamespace() {
        return client.getNamespace();
    }

    @Override
    public String getUserId(String userName) {
        User user = client.users().withName(userName).get();
        if (user != null) {
            return user.getMetadata().getUid();
        }
        return null;
    }

}
