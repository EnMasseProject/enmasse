/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AuthenticationService;

import java.util.Optional;

/**
 * Interface for resolving different authentication service
 */
public interface AuthenticationServiceRegistry {
    Optional<io.enmasse.admin.model.v1.AuthenticationService> findAuthenticationService(AuthenticationService authenticationService);
    Optional<io.enmasse.admin.model.v1.AuthenticationService> resolveDefaultAuthenticationService();
}
