package io.enmasse.controller.api.osb.v2;

import org.jboss.resteasy.util.BasicAuthHelper;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class BasicAuthInterceptor implements ContainerRequestFilter {

    private final PasswordAuthentication passwordAuthentication;
    private final String baseUri;

    public BasicAuthInterceptor(PasswordAuthentication passwordAuthentication, String baseUri) {
        this.passwordAuthentication = passwordAuthentication;
        this.baseUri = baseUri;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!requestContext.getUriInfo().getRequestUri().getPath().startsWith(baseUri)) {
            return;
        }

        String auth = requestContext.getHeaderString("authorization");

        if (auth == null) {
            throw OSBExceptions.notAuthorizedException();
        }

        String[] userPass = BasicAuthHelper.parseHeader(auth);
        if (userPass == null || userPass.length != 2) {
            throw OSBExceptions.notAuthorizedException();
        }

        if (!isAuthenticated(userPass[0], userPass[1])) {
            throw OSBExceptions.notAuthorizedException();
        }
    }

    private boolean isAuthenticated(String user, String pass) {
        return passwordAuthentication.getUserName().equals(user)
                && Arrays.equals(passwordAuthentication.getPassword(), pass.toCharArray());
    }
}
