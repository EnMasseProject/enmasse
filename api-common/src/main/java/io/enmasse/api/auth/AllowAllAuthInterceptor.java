/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

public class AllowAllAuthInterceptor implements ContainerRequestFilter {

    private static final String X_REMOTE_USER = "X-Remote-User";
    private final AuthApi authApi;
    private final boolean doUserLookup;

    public AllowAllAuthInterceptor(AuthApi authApi, boolean doUserLookup) {
        this.authApi = authApi;
        this.doUserLookup = doUserLookup;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (doUserLookup && requestContext.getHeaderString(X_REMOTE_USER) != null) {
            String userName = requestContext.getHeaderString(X_REMOTE_USER);
            String userId = authApi.getUserId(userName);

            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return RbacSecurityContext.getUserPrincipal(userName, userId);
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
        } else {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return RbacSecurityContext.getUserPrincipal("anonymous", "");
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
}
