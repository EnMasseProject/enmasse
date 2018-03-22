/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceResolver;

import java.util.Optional;

/**
 * Resolves the none authentication service host name.
 */
public class StandardAuthenticationServiceResolver implements AuthenticationServiceResolver {
    private final String host;
    private final int port;

    public StandardAuthenticationServiceResolver(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost(AuthenticationService authService) {
        String overrideHost = (String) authService.getDetails().get("host");
        if (overrideHost != null) {
            return overrideHost;
        } else {
            return host;
        }
    }

    @Override
    public int getPort(AuthenticationService authService) {
        String overridePort = (String) authService.getDetails().get("port");
        if (overridePort != null) {
            return Integer.parseInt(overridePort);
        } else {
            return port;
        }
    }

    @Override
    public Optional<String> getCaSecretName(AuthenticationService authService) {
        return Optional.of("standard-authservice-cert");
    }

    @Override
    public Optional<String> getClientSecretName(AuthenticationService authService) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSaslInitHost(String addressSpaceName, AuthenticationService authService) {
        return Optional.of(addressSpaceName);
    }
}
