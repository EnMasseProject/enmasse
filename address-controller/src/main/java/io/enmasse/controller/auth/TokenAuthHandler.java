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

    /**
     * The auth handler will check whether the user associated with the token has access to this nonResourceURL (one of
     * the ClusterRoles bound to the user must grant access to this nonResourceURL)
     */
    private String authorizationNonResourceUrlPath;


    public TokenAuthHandler(Kubernetes kubernetes, String authorizationNonResourceUrlPath) {
        this.kubernetes = kubernetes;
        this.authorizationNonResourceUrlPath = authorizationNonResourceUrlPath;
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

        SubjectAccessReview subjectAccessReview = kubernetes.performSubjectAccessReview(username, authorizationNonResourceUrlPath, requestContext.getMethod().toLowerCase());
        return subjectAccessReview.getStatus().getAllowed();
    }

}
