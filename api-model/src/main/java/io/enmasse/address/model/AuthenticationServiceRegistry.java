/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Optional;

/**
 * Interface for resolving different authentication service
 */
public interface AuthenticationServiceRegistry {
    Optional<io.enmasse.admin.model.v1.AuthenticationService> findAuthenticationService(AuthenticationService authenticationService);
}
