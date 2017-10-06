package io.enmasse.controller.auth;

import io.enmasse.controller.common.Kubernetes;
import io.fabric8.kubernetes.api.model.authentication.TokenReview;
import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;

public class TokenAuthHandler implements AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthHandler.class.getName());

    public static final String BEARER_PREFIX = "Bearer ";

    private final Kubernetes kubernetes;


    public TokenAuthHandler(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    @Override
    public boolean isAuthenticated(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString("authorization");

        if (auth == null) {
            return false;
        }

        if (!auth.startsWith(BEARER_PREFIX)) {
            return false;
        }

        String token = auth.substring(BEARER_PREFIX.length());

        TokenReview tokenReview = kubernetes.performTokenReview(token);
        if (!tokenReview.getStatus().getAuthenticated()) {
            return false;
        }

        String username = tokenReview.getStatus().getUser().getUsername();

        SubjectAccessReview subjectAccessReview = kubernetes.performSubjectAccessReview(username, "/enmasse-broker", requestContext.getMethod().toLowerCase());
        return subjectAccessReview.getStatus().getAllowed();
    }

}
