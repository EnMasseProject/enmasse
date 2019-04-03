/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;

import java.util.List;
import java.util.Optional;

/**
 * Interface for resolving different authentication service
 */
public interface AuthenticationServiceRegistry {
    Optional<io.enmasse.admin.model.v1.AuthenticationService> findAuthenticationService(AuthenticationService authenticationService);
    Optional<io.enmasse.admin.model.v1.AuthenticationService> findAuthenticationServiceByName(String name);
    List<io.enmasse.admin.model.v1.AuthenticationService> findAuthenticationServiceByType(AuthenticationServiceType type);
    Optional<io.enmasse.admin.model.v1.AuthenticationService> resolveDefaultAuthenticationService();
    List<io.enmasse.admin.model.v1.AuthenticationService> listAuthenticationServices();
}
