package io.enmasse.controller.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.enmasse.controller.common.Kubernetes;
import io.fabric8.kubernetes.api.model.authentication.TokenReview;
import io.fabric8.kubernetes.api.model.authentication.TokenReviewBuilder;
import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.SubjectAccessReviewBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;


public class TokenAuthHandlerTest {

    private File tokenFile;
    private TokenAuthHandler handler;
    private ContainerRequestContext mockRequestContext;
    private Kubernetes mockKubernetes;

    @Before
    public void setUp() throws IOException {
        tokenFile = File.createTempFile("token", "");
        mockKubernetes = mock(Kubernetes.class);
        handler = new TokenAuthHandler(mockKubernetes, "/enmasse-broker");
        mockRequestContext = mock(ContainerRequestContext.class);
    }

    @Test
    public void testNoAuthorizationHeader() {
        when(mockRequestContext.getHeaderString("authorization")).thenReturn(null);
        assertThat(handler.isAuthenticated(mockRequestContext), is(false));
    }

    @Test
    public void testBasicAuth() throws IOException {
        Files.write(Paths.get(tokenFile.getAbsolutePath()), "valid_token".getBytes());
        when(mockRequestContext.getHeaderString("authorization")).thenReturn("Basic dXNlcjpwYXNzCg==");
        assertThat(handler.isAuthenticated(mockRequestContext), is(false));
    }

    @Test
    public void testInvalidToken() throws IOException {
        TokenReview returnedTokenReview = new TokenReviewBuilder()
                .withNewSpec().withToken("invalid_token").endSpec()
                .withNewStatus().withAuthenticated(false).endStatus()
                .build();
        when(mockKubernetes.performTokenReview("invalid_token")).thenReturn(returnedTokenReview);
        when(mockRequestContext.getHeaderString("authorization")).thenReturn("Bearer invalid_token");
        assertThat(handler.isAuthenticated(mockRequestContext), is(false));
    }

    @Test
    public void testValidTokenButNotAuthorized() throws IOException {
        TokenReview returnedTokenReview = new TokenReviewBuilder()
                .withNewSpec().withToken("valid_token").endSpec()
                .withNewStatus().withAuthenticated(true).withNewUser().withUsername("foo").endUser().endStatus()
                .build();
        when(mockKubernetes.performTokenReview("valid_token")).thenReturn(returnedTokenReview);
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReviewBuilder()
                .withNewSpec().withUser("foo").endSpec()
                .withNewStatus().withAllowed(false).endStatus()
                .build();
        when(mockKubernetes.performSubjectAccessReview("foo", "/enmasse-broker", "post")).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString("authorization")).thenReturn("Bearer valid_token");
        when(mockRequestContext.getMethod()).thenReturn(HttpMethod.POST);
        assertThat(handler.isAuthenticated(mockRequestContext), is(false));
    }

    @Test
    public void testAuthorized() throws IOException {
        TokenReview returnedTokenReview = new TokenReviewBuilder()
                .withNewSpec().withToken("valid_token").endSpec()
                .withNewStatus().withAuthenticated(true).withNewUser().withUsername("foo").endUser().endStatus()
                .build();
        when(mockKubernetes.performTokenReview("valid_token")).thenReturn(returnedTokenReview);
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReviewBuilder()
                .withNewSpec().withUser("foo").endSpec()
                .withNewStatus().withAllowed(true).endStatus()
                .build();
        when(mockKubernetes.performSubjectAccessReview("foo", "/enmasse-broker", "post")).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString("authorization")).thenReturn("Bearer valid_token");
        when(mockRequestContext.getMethod()).thenReturn(HttpMethod.POST);
        assertThat(handler.isAuthenticated(mockRequestContext), is(true));
    }
}
