/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Optional;

import static io.enmasse.api.auth.AuthInterceptor.X_REMOTE_USER;

public class AllowAllAuthInterceptor implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String username = Optional.ofNullable(requestContext.getHeaderString(X_REMOTE_USER)).orElse("system:anonymous");
        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return RbacSecurityContext.getUserPrincipal(username, "");
            }

            @Override
            public boolean isUserInRole(String role) {
                return true;
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return "dummy";
            }
        });
    }
}
