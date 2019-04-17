/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class KubeAuthApi implements AuthApi {
    private static final Logger log = LoggerFactory.getLogger(KubeAuthApi.class);
    private final NamespacedKubernetesClient client;
    private final String namespace;
    private final String apiToken;

    public KubeAuthApi(NamespacedKubernetesClient client, String apiToken) {
        this.client = client;
        this.namespace = client.getNamespace();
        this.apiToken = apiToken;
    }

    private JsonObject doRawHttpRequest(String path, String method, JsonObject body, boolean errorOk) {
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);

        HttpUrl url = HttpUrl.get(client.getMasterUrl()).resolve(path);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiToken)
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

    @Override
    public TokenReview performTokenReview(String token) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "TokenReview");
            body.put("apiVersion", "authentication.k8s.io/v1");

            JsonObject spec = new JsonObject();
            spec.put("token", token);
            body.put("spec", spec);

            log.debug("Token review request: {}", body);
            JsonObject responseBody= doRawHttpRequest("/apis/authentication.k8s.io/v1/tokenreviews", "POST", body, false);
            log.debug("Token review response: {}", responseBody);
            JsonObject status = responseBody.getJsonObject("status");
            boolean authenticated = false;
            String userName = null;
            String userId = null;
            Set<String> groups = null;
            Map<String, List<String>> extra = null;
            if (status != null) {
                Boolean auth = status.getBoolean("authenticated");
                authenticated = auth == null ? false : auth;
                JsonObject user = status.getJsonObject("user");
                if (user != null) {
                    userName = user.getString("username");
                    userId = user.getString("uid");
                    JsonArray groupArray = user.getJsonArray("groups");
                    if (groupArray != null) {
                        groups = new HashSet<>();
                        for (int i = 0; i < groupArray.size(); i++) {
                            groups.add(groupArray.getString(i));
                        }
                    }

                    JsonObject extraObject = user.getJsonObject("extra");
                    if (extraObject != null) {
                        extra = new HashMap<>();
                        for (String field : extraObject.fieldNames()) {
                            JsonArray extraValues = extraObject.getJsonArray(field);
                            List<String> extraValuesList = new ArrayList<>();
                            for (int i = 0; i < extraValues.size(); i++) {
                                extraValuesList.add(extraValues.getString(i));
                            }
                            extra.put(field, extraValuesList);
                        }
                    }

                }
            }
            return new TokenReview(userName, userId, groups, extra, authenticated);
        } else {
            return new TokenReview(null, null, null, null, false);
        }
    }

    private static void putCommonSpecAttributes(JsonObject spec, TokenReview tokenReview) {
        if (tokenReview.getUserName() != null && !tokenReview.getUserName().isEmpty()) {
            spec.put("user", tokenReview.getUserName());
        }
        if (tokenReview.getUserId() != null && !tokenReview.getUserId().isEmpty()) {
            spec.put("uid", tokenReview.getUserId());
        }
        if (tokenReview.getGroups() != null && !tokenReview.getGroups().isEmpty()) {
            JsonArray groups = new JsonArray();
            for (String group : tokenReview.getGroups()) {
                groups.add(group);
            }
            spec.put("groups", groups);
        }

        if (tokenReview.getExtra() != null && !tokenReview.getExtra().isEmpty()) {
            JsonObject extra = new JsonObject();
            for (Map.Entry<String, List<String>> entry : tokenReview.getExtra().entrySet()) {
                JsonArray entryArray = new JsonArray();
                for (String value : entry.getValue()) {
                    entryArray.add(value);
                }
                extra.put(entry.getKey(), entryArray);
            }
            spec.put("extra", extra);
        }
    }

    @Override
    public io.enmasse.api.auth.SubjectAccessReview performSubjectAccessReviewPath(TokenReview tokenReview, String path, String verb) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "SubjectAccessReview");
            body.put("apiVersion", "authorization.k8s.io/v1");

            JsonObject spec = new JsonObject();

            JsonObject nonResourceAttributes = new JsonObject();
            nonResourceAttributes.put("path", path);
            nonResourceAttributes.put("verb", verb);

            spec.put("nonResourceAttributes", nonResourceAttributes);

            putCommonSpecAttributes(spec, tokenReview);

            body.put("spec", spec);
            log.debug("Subject access review request: {}", body);
            JsonObject responseBody = doRawHttpRequest("/apis/authorization.k8s.io/v1/subjectaccessreviews", "POST", body, false);

            log.debug("Subject access review response: {}", responseBody);
            JsonObject status = responseBody.getJsonObject("status");
            boolean allowed = false;
            if (status != null) {
                Boolean allowedMaybe = status.getBoolean("allowed");
                allowed = allowedMaybe == null ? false : allowedMaybe;
            }
            return new io.enmasse.api.auth.SubjectAccessReview(tokenReview.getUserName(), allowed);
        } else {
            return new SubjectAccessReview(tokenReview.getUserName(), false);
        }
    }

    @Override
    public io.enmasse.api.auth.SubjectAccessReview performSubjectAccessReviewResource(TokenReview tokenReview, String namespace, String resource, String verb, String apiGroup) {
        if (client.isAdaptable(OkHttpClient.class)) {
            JsonObject body = new JsonObject();

            body.put("kind", "SubjectAccessReview");
            body.put("apiVersion", "authorization.k8s.io/v1");

            JsonObject spec = new JsonObject();

            JsonObject resourceAttributes = new JsonObject();
            resourceAttributes.put("group", apiGroup);
            resourceAttributes.put("namespace", namespace);
            resourceAttributes.put("resource", resource);
            resourceAttributes.put("verb", verb);

            spec.put("resourceAttributes", resourceAttributes);

            putCommonSpecAttributes(spec, tokenReview);

            body.put("spec", spec);
            log.debug("Subject access review request: {}", body);
            JsonObject responseBody = doRawHttpRequest("/apis/authorization.k8s.io/v1/subjectaccessreviews", "POST", body, false);
            log.debug("Subject access review response: {}", responseBody);

            JsonObject status = responseBody.getJsonObject("status");
            boolean allowed = false;
            if (status != null) {
                Boolean allowedMaybe = status.getBoolean("allowed");
                allowed = allowedMaybe == null ? false : allowedMaybe;
            }
            return new io.enmasse.api.auth.SubjectAccessReview(tokenReview.getUserName(), allowed);
        } else {
            return new SubjectAccessReview(tokenReview.getUserName(), false);
        }
    }

    @Override
    public String getCert(String secretName) {
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
}
