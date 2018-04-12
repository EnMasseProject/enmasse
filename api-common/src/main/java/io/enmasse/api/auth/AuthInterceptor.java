/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.enmasse.api.common.Exceptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;

public class AuthInterceptor implements ContainerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";
    private final AuthApi authApi;
    private final String [] omittedPaths;

    public AuthInterceptor(AuthApi authApi, String ... omittedPaths) {
        this.authApi = authApi;
        this.omittedPaths = omittedPaths;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        for (String omittedPath : omittedPaths) {
            if (requestContext.getUriInfo().getPath().startsWith(omittedPath)) {
                return;
            }
        }
        boolean isAuthenticated = false;
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            String token = auth.substring(BEARER_PREFIX.length());
            TokenReview tokenReview = authApi.performTokenReview(token);
            isAuthenticated = tokenReview.isAuthenticated();
            if (isAuthenticated) {
                requestContext.setSecurityContext(new RbacSecurityContext(tokenReview, authApi, requestContext.getUriInfo()));
            }
        }
        if (!isAuthenticated) {
            throw Exceptions.notAuthorizedException();
        }
    }
}
