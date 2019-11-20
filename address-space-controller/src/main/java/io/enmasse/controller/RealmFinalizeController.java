/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.user.api.RealmApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class RealmFinalizeController extends AbstractFinalizeController {
    private static final Logger log = LoggerFactory.getLogger(AddressFinalizerController.class);
    public static final String FINALIZER_REALMS = "enmasse.io/realms";

    private final RealmApi realmApi;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;

    public RealmFinalizeController(RealmApi realmApi, AuthenticationServiceRegistry authenticationServiceRegistry) {
        super(FINALIZER_REALMS);
        this.realmApi = realmApi;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
    }

    @Override
    public String toString() {
        return "RealmFinalizeController";
    }

    @Override
    protected Result processFinalizer(AddressSpace addressSpace) {
        log.info("Processing realm finalizer for {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());

        final AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService()).orElse(null);
        final String realmName = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
        // Only remove realms on authentication services of type standard where realm is per-address space
        if (authenticationService != null &&
                authenticationService.getStatus() != null &&
                authenticationService.getSpec().getType().equals(AuthenticationServiceType.standard) &&
                authenticationService.getSpec().getRealm() == null &&
                realmName != null) {

            try {
                deleteRealm(authenticationService, realmName);
            } catch (Exception e) {
                log.warn("Error finalizing {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), e);
                return Result.waiting(addressSpace);
            }
        }
        return Result.completed(addressSpace);
    }

    private void deleteRealm(AuthenticationService authenticationService, String realmName) throws Exception {
        Set<String> actualRealms = realmApi.getRealmNames(authenticationService);
        if (actualRealms.contains(realmName)) {
            log.info("Deleting realm {}", realmName);
            realmApi.deleteRealm(authenticationService, realmName);
        }
    }
}
