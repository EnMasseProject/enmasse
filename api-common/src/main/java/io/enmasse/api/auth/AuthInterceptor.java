/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.enmasse.api.common.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;

public class AuthInterceptor implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    public static final String BEARER_PREFIX = "Bearer ";
    private final AuthApi authApi;
    private final Predicate<String> pathFilter;

    public AuthInterceptor(AuthApi authApi, Predicate<String> pathFilter) {
        this.authApi = authApi;
        this.pathFilter = pathFilter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (pathFilter.test(requestContext.getUriInfo().getPath())) {
            return;
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
