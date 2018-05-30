/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.enmasse.api.common.Exceptions;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Predicate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

public class AuthInterceptor implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    public static final String BEARER_PREFIX = "Bearer ";
    private final AuthApi authApi;
    private final Predicate<String> pathFilter;
    public static final String X_REMOTE_USER = "X-Remote-User";

    @Context
    private HttpServerRequest request;

    void setRequest(HttpServerRequest request) {
        this.request = request;
    }

    public AuthInterceptor(AuthApi authApi, Predicate<String> pathFilter) {
        this.authApi = authApi;
        this.pathFilter = pathFilter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (pathFilter.test(requestContext.getUriInfo().getPath())) {
            return;
        }

        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            log.debug("Authentication using bearer token");
            String token = auth.substring(BEARER_PREFIX.length());
            TokenReview tokenReview = authApi.performTokenReview(token);
            if (tokenReview.isAuthenticated()) {
                requestContext.setSecurityContext(new RbacSecurityContext(tokenReview, authApi, requestContext.getUriInfo()));
            } else {
                throw Exceptions.notAuthorizedException();
            }
        } else if (request != null && request.isSSL() && requestContext.getHeaderString(X_REMOTE_USER) != null) {
            log.debug("Authenticating using client certificate");
            HttpConnection connection = request.connection();
            String userName = requestContext.getHeaderString(X_REMOTE_USER);
            try {
                connection.peerCertificateChain();
                log.debug("Client certificates trusted... impersonating {}", userName);
                requestContext.setSecurityContext(new RbacSecurityContext(new TokenReview(userName, "", true), authApi, requestContext.getUriInfo()));
            } catch (SSLPeerUnverifiedException e) {
                log.debug("Peer certificate not valid, proceeding as anonymous");
                requestContext.setSecurityContext(new RbacSecurityContext(new TokenReview("system:anonymous", "", false), authApi, requestContext.getUriInfo()));
            }
        } else {
            requestContext.setSecurityContext(new RbacSecurityContext(new TokenReview("system:anonymous", "", false), authApi, requestContext.getUriInfo()));
        }
    }
}
