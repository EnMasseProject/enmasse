/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api;

import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.controller.api.v1.http.HttpHealthService;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.TokenReview;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;

public class AuthInterceptor implements ContainerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";
    private final Kubernetes kubernetes;

    public AuthInterceptor(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContext.getUriInfo().getPath().equals(HttpHealthService.BASE_URI)) {
            return;
        }
        boolean isAuthenticated = false;
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            String token = auth.substring(BEARER_PREFIX.length());
            TokenReview tokenReview = kubernetes.performTokenReview(token);
            isAuthenticated = tokenReview.isAuthenticated();
            if (isAuthenticated) {
                requestContext.setSecurityContext(new RbacSecurityContext(tokenReview, kubernetes, requestContext.getUriInfo()));
            }
        }
        if (!isAuthenticated) {
            throw OSBExceptions.notAuthorizedException();
        }
    }
}
