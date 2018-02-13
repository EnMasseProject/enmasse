/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.enmasse.controller.api.AuthInterceptor;
import io.enmasse.controller.api.RbacSecurityContext;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.v1.http.HttpHealthService;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.SubjectAccessReview;
import io.enmasse.controller.common.TokenReview;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;


public class AuthInterceptorTest {

    private File tokenFile;
    private AuthInterceptor handler;
    private ContainerRequestContext mockRequestContext;
    private UriInfo mockUriInfo;
    private Kubernetes mockKubernetes;

    @Before
    public void setUp() throws IOException {
        tokenFile = File.createTempFile("token", "");
        mockKubernetes = mock(Kubernetes.class);
        when(mockKubernetes.getNamespace()).thenReturn("myspace");
        handler = new AuthInterceptor(mockKubernetes);
        mockRequestContext = mock(ContainerRequestContext.class);

        mockUriInfo = mock(UriInfo.class);
        when(mockUriInfo.getAbsolutePath()).thenReturn(URI.create("https://localhost:443/apis/enmasse.io/v1/addressspaces"));
        when(mockUriInfo.getPath()).thenReturn("/apis/enmasse.io/v1/addressspaces");
        when(mockRequestContext.getUriInfo()).thenReturn(mockUriInfo);
    }


    private void assertExceptionThrown(ContainerRequestContext requestContext) {
        try {
            handler.filter(requestContext);
            fail("Expected exception to be thrown");
        } catch (Exception e) {
        }
    }

    @Test
    public void testNoAuthorizationHeader() {
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        assertExceptionThrown(mockRequestContext);
    }

    @Test
    public void testBasicAuth() throws IOException {
        Files.write(Paths.get(tokenFile.getAbsolutePath()), "valid_token".getBytes());
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNzCg==");
        assertExceptionThrown(mockRequestContext);
    }

    @Test
    public void testInvalidToken() throws IOException {
        TokenReview returnedTokenReview = new TokenReview(null, false);
        when(mockKubernetes.performTokenReview("invalid_token")).thenReturn(returnedTokenReview);
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid_token");
        assertExceptionThrown(mockRequestContext);
    }

    @Test
    public void testValidTokenButNotAuthorized() throws IOException {
        TokenReview returnedTokenReview = new TokenReview("foo", true);
        when(mockKubernetes.performTokenReview("valid_token")).thenReturn(returnedTokenReview);
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReview("foo", false);
        when(mockKubernetes.performSubjectAccessReview(eq("foo"), any(), eq("create"), any())).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(mockRequestContext.getMethod()).thenReturn(HttpMethod.POST);

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        handler.filter(mockRequestContext);
        verify(mockRequestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();

        assertNotNull(context);
        assertThat(context.getAuthenticationScheme(), is("RBAC"));
        RbacSecurityContext rbacSecurityContext = (RbacSecurityContext) context;
        assertThat(rbacSecurityContext.getUserPrincipal().getName(), is("foo"));
        assertFalse(rbacSecurityContext.isUserInRole(RbacSecurityContext.rbacToRole("myspace", ResourceVerb.create, null)));
    }

    @Test
    public void testHealthAuthz() throws IOException {
        when(mockUriInfo.getPath()).thenReturn(HttpHealthService.BASE_URI);
        handler.filter(mockRequestContext);
    }

    @Test
    public void testAuthorized() throws IOException {
        TokenReview returnedTokenReview = new TokenReview("foo", true);
        when(mockKubernetes.performTokenReview("valid_token")).thenReturn(returnedTokenReview);
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReview("foo", true);
        when(mockKubernetes.performSubjectAccessReview(eq("foo"), any(), eq("create"), any())).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(mockRequestContext.getMethod()).thenReturn(HttpMethod.POST);

        handler.filter(mockRequestContext);

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(mockRequestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();

        assertThat(context.getAuthenticationScheme(), is("RBAC"));
        RbacSecurityContext rbacSecurityContext = (RbacSecurityContext) context;
        assertThat(rbacSecurityContext.getUserPrincipal().getName(), is("foo"));
        assertTrue(rbacSecurityContext.isUserInRole(RbacSecurityContext.rbacToRole("myspace", ResourceVerb.create, null)));
    }
}
