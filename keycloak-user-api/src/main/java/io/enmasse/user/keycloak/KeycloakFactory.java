/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import io.enmasse.admin.model.v1.AuthenticationService;
import org.keycloak.admin.client.Keycloak;

public interface KeycloakFactory {
    Keycloak createInstance(AuthenticationService authenticationService);
}
