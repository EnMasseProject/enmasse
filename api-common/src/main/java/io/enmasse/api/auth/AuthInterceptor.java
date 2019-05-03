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

import java.util.*;
import java.util.function.Predicate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

public class AuthInterceptor implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    public static final String BEARER_PREFIX = "Bearer ";
    private final AuthApi authApi;
    private final ApiHeaderConfig apiHeaderConfig;
    private final Predicate<String> pathFilter;

    @Context
    private HttpServerRequest request;

    void setRequest(HttpServerRequest request) {
        this.request = request;
    }

    public AuthInterceptor(AuthApi authApi, ApiHeaderConfig apiHeaderConfig, Predicate<String> pathFilter) {
        this.authApi = authApi;
        this.apiHeaderConfig = apiHeaderConfig;
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
        } else if (request != null && request.isSSL() && findUserName(requestContext) != null) {
            log.debug("Authenticating using client certificate");
            HttpConnection connection = request.connection();
            String userName = findUserName(requestContext);
            String group = findGroup(requestContext);
            Map<String, List<String>> extras = findExtra(requestContext);
            log.info("Found username {}, group {}, extra {}", userName, group, extras);
            try {
                connection.peerCertificateChain();
                log.debug("Client certificates trusted... impersonating {}", userName);
                requestContext.setSecurityContext(new RbacSecurityContext(new TokenReview(userName, "", Collections.singleton(group), extras, true), authApi, requestContext.getUriInfo()));
            } catch (SSLPeerUnverifiedException e) {
                log.debug("Peer certificate not valid, proceeding as anonymous");
                requestContext.setSecurityContext(new RbacSecurityContext(new TokenReview("system:anonymous", "", null, null, false), authApi, requestContext.getUriInfo()));
            }
        } else {
            requestContext.setSecurityContext(new RbacSecurityContext(new TokenReview("system:anonymous", "", null, null, false), authApi, requestContext.getUriInfo()));
        }
    }

    private Map<String, List<String>> findExtra(ContainerRequestContext requestContext) {
        Map<String, List<String>> extras = new HashMap<>();
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        if (headers != null) {
            for (String extraHeaderPrefix : apiHeaderConfig.getExtraHeadersPrefix()) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    if (entry.getKey().startsWith(extraHeaderPrefix)) {
                        String key = entry.getKey().substring(extraHeaderPrefix.length()).toLowerCase();
                        extras.put(key, entry.getValue());
                    }
                }
            }
        }
        return extras;
    }

    private String findUserName(ContainerRequestContext requestContext) {
        for (String userHeader : apiHeaderConfig.getUserHeaders()) {
            if (requestContext.getHeaderString(userHeader) != null) {
                return requestContext.getHeaderString(userHeader);
            }
        }
        return null;
    }

    private String findGroup(ContainerRequestContext requestContext) {
        for (String groupHeader : apiHeaderConfig.getGroupHeaders()) {
            if (requestContext.getHeaderString(groupHeader) != null) {
                return requestContext.getHeaderString(groupHeader);
            }
        }
        return null;
    }
}
