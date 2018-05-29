/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.vertx.core.json.JsonObject;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;

public class RbacSecurityContext implements SecurityContext {
    private final TokenReview tokenReview;
    private final AuthApi authApi;
    private final UriInfo uriInfo;

    public RbacSecurityContext(TokenReview tokenReview, AuthApi authApi, UriInfo uriInfo) {
        this.tokenReview = tokenReview;
        this.authApi = authApi;
        this.uriInfo = uriInfo;
    }

    @Override
    public Principal getUserPrincipal() {
        return getUserPrincipal(tokenReview.getUserName(), tokenReview.getUserId());
    }

    public static Principal getUserPrincipal(String username, String uid) {
        return () -> {
            JsonObject object = new JsonObject();
            object.put("username", username);
            object.put("uid", uid);
            return object.encode();
        };
    }

    public static String getUserName(Principal principal) {
        JsonObject object = new JsonObject(principal.getName());
        return object.getString("username");
    }

    public static String getUserId(Principal principal) {
        JsonObject object = new JsonObject(principal.getName());
        return object.getString("uid");
    }

    @Override
    public boolean isUserInRole(String json) {
        JsonObject data = new JsonObject(json);
        SubjectAccessReview accessReview;
        String type = data.getString("type");

        if ("path".equals(type)) {
            String path = data.getString("path");
            String verb = data.getString("verb");
            accessReview = authApi.performSubjectAccessReviewPath(tokenReview.getUserName(), path, verb);
        } else if ("resource".equals(type)) {
            String namespace = data.getString("namespace");
            String verb = data.getString("verb");
            String resource = data.getString("resource");
            accessReview = authApi.performSubjectAccessReviewResource(tokenReview.getUserName(), namespace, resource, verb);
        } else {
            throw new IllegalArgumentException("Unknown role type " + type);
        }
        return accessReview.isAllowed();
    }

    public static String rbacToRole(String namespace, ResourceVerb verb, String resource) {
        JsonObject object = new JsonObject();
        object.put("type",  "resource");
        object.put("namespace", namespace);
        object.put("verb", verb);
        object.put("resource", resource);
        return object.toString();
    }

    public static String rbacToRole(String path, String verb) {
        JsonObject object = new JsonObject();
        object.put("type",  "path");
        object.put("path", path);
        object.put("verb", verb);
        return object.toString();
    }

    @Override
    public boolean isSecure() {
        return uriInfo.getAbsolutePath().toString().startsWith("https");
    }

    @Override
    public String getAuthenticationScheme() {
        return "RBAC";
    }
}
