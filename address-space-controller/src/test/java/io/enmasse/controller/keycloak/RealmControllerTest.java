/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.user.api.RealmApi;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RealmControllerTest {

    private RealmController manager;
    private Set<String> realms;
    private AuthenticationServiceRegistry mockAuthenticationServiceRegistry;

    @BeforeEach
    public void setup() {
        realms = new HashSet<>();
        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .endMetadata()
                .withNewSpec()
                .withType(io.enmasse.admin.model.v1.AuthenticationServiceType.standard)
                .endSpec()
                .withNewStatus()
                .withHost("example.com")
                .withPort(5671)
                .endStatus()
                .build();
        mockAuthenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        io.enmasse.address.model.AuthenticationService standardSvc = new io.enmasse.address.model.AuthenticationServiceBuilder()
                .withName("standard")
                .build();
        when(mockAuthenticationServiceRegistry.findAuthenticationService(standardSvc)).thenReturn(Optional.of(authenticationService));
        when(mockAuthenticationServiceRegistry.findAuthenticationServiceByType(eq(io.enmasse.admin.model.v1.AuthenticationServiceType.standard))).thenReturn(Arrays.asList(authenticationService));
        when(mockAuthenticationServiceRegistry.listAuthenticationServices()).thenReturn(Collections.singletonList(authenticationService));

        manager = new RealmController(new RealmApi() {
            @Override
            public Set<String> getRealmNames(AuthenticationService auth) {
                return new HashSet<>(realms);
            }

            @Override
            public void createRealm(AuthenticationService auth, String namespace, String realmName) {
                realms.add(realmName);
            }

            @Override
            public void deleteRealm(AuthenticationService auth, String realmName) {
                realms.remove(realmName);
            }
        }, mockAuthenticationServiceRegistry);
    }

    @Test
    public void testAddAddressSpace() throws Exception {
        manager.reconcileAll(Collections.singletonList(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertTrue(realms.isEmpty());

        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));

        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testRemoveAddressSpace() throws Exception {
        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a2", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));
        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD), createAddressSpace("a3", AuthenticationServiceType.STANDARD)));

        assertTrue(realms.contains("a1"));
        assertFalse(realms.contains("a2"));
        assertTrue(realms.contains("a3"));
        assertEquals(2, realms.size());
    }

    @Test
    public void testAuthTypeChanged() throws Exception {
        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.STANDARD)));
        assertTrue(realms.contains("a1"));
        assertEquals(1, realms.size());

        manager.reconcileAll(Arrays.asList(createAddressSpace("a1", AuthenticationServiceType.NONE)));
        assertFalse(realms.contains("a1"));
        assertEquals(0, realms.size());
    }

    private AddressSpace createAddressSpace(String name, AuthenticationServiceType authType) {
        return new AddressSpaceBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .withNamespace("myns")
                        .addToAnnotations(AnnotationKeys.CREATED_BY, "developer")
                        .addToAnnotations(AnnotationKeys.REALM_NAME, name)
                        .build())

                .withNewSpec()
                .withPlan("myplan")
                .withType("standard")

                .addToEndpoints(new EndpointSpecBuilder()
                        .withName("console")
                        .withService("console")
                        .build())
                .withAuthenticationService(new io.enmasse.address.model.AuthenticationServiceBuilder().withName(authType.getName()).build())
                .endSpec()

                .withNewStatus()
                    .withReady(true)
                    .addToEndpointStatuses(new EndpointStatusBuilder()
                            .withName("console")
                            .withServiceHost("console.svc")
                            .withExternalPorts(Collections.singletonMap("http", 443))
                            .withExternalHost("console.example.com")
                            .build())
                .endStatus()

                .build();
    }
}
