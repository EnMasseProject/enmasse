package io.enmasse.controller.auth;

import javax.ws.rs.container.ContainerRequestContext;

public interface AuthHandler {
    boolean isAuthenticated(ContainerRequestContext requestContext);
}
