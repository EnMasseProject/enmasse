/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceSettings;
import io.enmasse.address.model.AuthenticationServiceSettingsBuilder;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;

public class AuthenticationServiceResolver {
    private final AuthenticationServiceRegistry registry;

    AuthenticationServiceResolver(AuthenticationServiceRegistry registry) {
        this.registry = registry;
    }

    AuthenticationServiceSettings resolve(AddressSpace addressSpace) {
        AuthenticationService authService = registry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService())
                .orElseThrow(() -> new IllegalArgumentException("Unable to find authentication service " + addressSpace.getSpec().getAuthenticationService()));

        if (authService.getStatus() == null) {
            throw new IllegalArgumentException("Authentication service '" + authService.getMetadata().getName() + "' is not yet deployed");
        }

        AuthenticationServiceSettingsBuilder settingsBuilder = new AuthenticationServiceSettingsBuilder();
        settingsBuilder.withHost(authService.getStatus().getHost());
        settingsBuilder.withPort(authService.getStatus().getPort());
        settingsBuilder.withRealm(authService.getSpec().getRealm() != null ? authService.getSpec().getRealm() : addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
        settingsBuilder.withCaCertSecret(authService.getStatus().getCaCertSecret());
        settingsBuilder.withClientCertSecret(authService.getStatus().getClientCertSecret());

        if (authService.getSpec().getType().equals(AuthenticationServiceType.external) && authService.getSpec().getExternal() != null && authService.getSpec().getExternal().isAllowOverride()) {
            if (addressSpace.getSpec().getAuthenticationService().getOverrides() != null) {
                AuthenticationServiceSettings overrides = addressSpace.getSpec().getAuthenticationService().getOverrides();
                if (overrides.getHost() != null) {
                    settingsBuilder.withHost(overrides.getHost());
                }
                if (overrides.getPort() != null) {
                    settingsBuilder.withPort(overrides.getPort());
                }
                if (overrides.getRealm() != null) {
                    settingsBuilder.withRealm(overrides.getRealm());
                }
                if (overrides.getCaCertSecret() != null) {
                    settingsBuilder.withCaCertSecret(overrides.getCaCertSecret());
                }
                if (overrides.getClientCertSecret() != null) {
                    settingsBuilder.withClientCertSecret(overrides.getClientCertSecret());
                }
            }
        }

        return settingsBuilder.build();
    }
}
