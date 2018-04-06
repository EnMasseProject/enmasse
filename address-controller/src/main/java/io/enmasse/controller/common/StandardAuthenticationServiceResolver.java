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
    private final String oauthUrl;
    private final String caSecretName;

    public StandardAuthenticationServiceResolver(String host, int port, String oauthUrl, String caSecretName) {
        this.host = host;
        this.port = port;
        this.oauthUrl = oauthUrl;
        this.caSecretName = caSecretName;
    }

    @Override
    public String getHost(AuthenticationService authService) {
        return host;
    }

    @Override
    public int getPort(AuthenticationService authService) {
        return port;
    }

    @Override
    public Optional<String> getCaSecretName(AuthenticationService authService) {
        return Optional.ofNullable(caSecretName);
    }

    @Override
    public Optional<String> getClientSecretName(AuthenticationService authService) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSaslInitHost(String addressSpaceName, AuthenticationService authService) {
        return Optional.of(addressSpaceName);
    }

    @Override
    public Optional<String> getOAuthURL(AuthenticationService authService) {
        return Optional.ofNullable(oauthUrl);
    }
}
