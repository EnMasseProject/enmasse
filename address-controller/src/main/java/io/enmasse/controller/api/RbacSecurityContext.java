/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.controller.api;

import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.SubjectAccessReview;
import io.enmasse.controller.common.TokenReview;
import io.vertx.core.json.JsonObject;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;

public class RbacSecurityContext implements SecurityContext {
    private final TokenReview tokenReview;
    private final Kubernetes kubernetes;
    private final UriInfo uriInfo;

    public RbacSecurityContext(TokenReview tokenReview, Kubernetes kubernetes, UriInfo uriInfo) {
        this.tokenReview = tokenReview;
        this.kubernetes = kubernetes;
        this.uriInfo = uriInfo;
    }

    @Override
    public Principal getUserPrincipal() {
        return tokenReview::getUserName;
    }

    @Override
    public boolean isUserInRole(String json) {
        JsonObject data = new JsonObject(json);

        String namespace = data.getString("namespace");
        String verb = data.getString("verb");
        String impersonateUser = data.getString("impersonateUser");
        SubjectAccessReview accessReview = kubernetes.performSubjectAccessReview(tokenReview.getUserName(), namespace, verb, impersonateUser);
        return accessReview.isAllowed();
    }

    public static String rbacToRole(String namespace, ResourceVerb verb, String impersonateUser) {
        JsonObject object = new JsonObject();
        object.put("namespace", namespace);
        object.put("verb", verb);
        object.put("impersonateUser", impersonateUser);
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
