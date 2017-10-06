package io.enmasse.controller.api;

import io.enmasse.controller.api.osb.v2.OSBExceptions;
import io.enmasse.controller.auth.AuthHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class AuthInterceptor implements ContainerRequestFilter {

    private final List<AuthHandler> authHandlers;
    private final String baseUri;

    public AuthInterceptor(AuthHandler authHandler, String baseUri) {
        this(Collections.singletonList(authHandler), baseUri);
    }

    public AuthInterceptor(List<AuthHandler> authHandlers, String baseUri) {
        this.authHandlers = authHandlers;
        this.baseUri = baseUri;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!requestContext.getUriInfo().getRequestUri().getPath().startsWith(baseUri)) {
            return;
        }

        for (AuthHandler authHandler : authHandlers) {
            if (authHandler.isAuthenticated(requestContext)) {
                return;
            }
        }
        throw OSBExceptions.notAuthorizedException();
    }

}
