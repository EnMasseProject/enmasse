/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.model.CustomResourceDefinitions;
import io.enmasse.user.api.RealmApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RealmFinalizerControllerTest {

    private RealmFinalizerController controller;
    private RealmApi realmApi;
    private AuthenticationServiceRegistry authenticationServiceRegistry;

    @BeforeAll
    public static void init() {
        CustomResourceDefinitions.registerAll();
    }

    @BeforeEach
    public void setup() {
        this.realmApi = mock(RealmApi.class);
        this.authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        this.controller = new RealmFinalizerController(realmApi, authenticationServiceRegistry);
    }

    @Test
    public void testFinalizerSuccess() throws Exception {
        AuthenticationService authenticationService = createAuthService(AuthenticationServiceType.standard, null);
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(realmApi.getRealmNames(eq(authenticationService))).thenReturn(Set.of("realm1", "realm2", "myrealm"));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace("standard", "myrealm"));
        assertNotNull(result);
        assertTrue(result.isFinalized());
        verify(realmApi).deleteRealm(eq(authenticationService), eq("myrealm"));
    }

    @Test
    public void testFinalizerAlreadyDeleted() throws Exception {
        AuthenticationService authenticationService = createAuthService(AuthenticationServiceType.standard, null);
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(realmApi.getRealmNames(eq(authenticationService))).thenReturn(Set.of("realm1", "realm2"));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace("standard", "myrealm"));
        assertNotNull(result);
        assertTrue(result.isFinalized());
        verify(realmApi, never()).deleteRealm(eq(authenticationService), eq("myrealm"));
    }

    @Test
    public void testFinalizerAuthServiceNotConfigured() throws Exception {
        AuthenticationService authenticationService = createAuthService(AuthenticationServiceType.standard, null);
        authenticationService.setStatus(null);
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(realmApi.getRealmNames(eq(authenticationService))).thenReturn(Set.of("realm1", "realm2", "myrealm"));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace("standard", "myrealm"));
        assertNotNull(result);
        assertTrue(result.isFinalized());
        verify(realmApi, never()).deleteRealm(eq(authenticationService), eq("myrealm"));
    }

    @Test
    public void testMissingAuthService() {
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace("unknown", "myrealm"));
        assertNotNull(result);
        assertTrue(result.isFinalized());
    }

    @Test
    public void testAuthServiceRealmSet() {
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(createAuthService(AuthenticationServiceType.standard, "myrealm")));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace("standard", "myrealm"));
        assertNotNull(result);
        assertTrue(result.isFinalized());
    }

    @Test
    public void testAuthServiceType() {
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(createAuthService(AuthenticationServiceType.none, null)));
        AbstractFinalizerController.Result result = controller.processFinalizer(createTestSpace("standard", "myrealm"));
        assertNotNull(result);
        assertTrue(result.isFinalized());
    }




    private static AuthenticationService createAuthService(AuthenticationServiceType type, String realm) {
        return new AuthenticationServiceBuilder()
                .editOrNewMetadata()
                .withName("standard")
                .endMetadata()
                .editOrNewSpec()
                .withType(type)
                .withRealm(realm)
                .endSpec()
                .editOrNewStatus()
                .endStatus()
                .build();
    }

    private static AddressSpace createTestSpace(String authenticationServiceName, String realm) {
        return new AddressSpaceBuilder()
                .editOrNewMetadata()
                .withName("myspace")
                .withNamespace("test")
                .addToAnnotations(AnnotationKeys.REALM_NAME, realm)
                .endMetadata()
                .editOrNewSpec()
                .editOrNewAuthenticationService()
                .withName(authenticationServiceName)
                .endAuthenticationService()
                .withType("standard")
                .withPlan("standard")
                .endSpec()
                .build();
    }
}
