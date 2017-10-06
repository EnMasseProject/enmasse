package io.enmasse.controller.auth;

import org.jboss.resteasy.util.BasicAuthHelper;

import javax.ws.rs.container.ContainerRequestContext;

public class BasicAuthHandler implements AuthHandler {

    private UserAuthenticator userAuthenticator;

    public BasicAuthHandler(UserAuthenticator userAuthenticator) {
        this.userAuthenticator = userAuthenticator;
    }

    @Override
    public boolean isAuthenticated(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString("authorization");

        if (auth == null) {
            return false;
        }

        String[] userPass = BasicAuthHelper.parseHeader(auth);
        if (userPass == null || userPass.length != 2) {
            return false;
        }

        return userAuthenticator.authenticate(userPass[0], userPass[1]);
    }
}
