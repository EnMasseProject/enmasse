/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Optional;

/**
 * Interface for resolving different authentication service
 */
public interface AuthenticationServiceResolver {
    String getHost(AuthenticationService authService);
    int getPort(AuthenticationService authService);

    Optional<String> getCaSecretName(AuthenticationService authService);

    Optional<String> getClientSecretName(AuthenticationService authService);

    Optional<String> getSaslInitHost(String addressSpaceName, AuthenticationService authService);
}
