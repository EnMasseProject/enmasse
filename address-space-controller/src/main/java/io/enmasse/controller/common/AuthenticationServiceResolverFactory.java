/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AuthenticationServiceResolver;
import io.enmasse.address.model.AuthenticationServiceType;

public interface AuthenticationServiceResolverFactory {
    AuthenticationServiceResolver getResolver(AuthenticationServiceType authenticationServiceType);
}
