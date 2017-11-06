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
