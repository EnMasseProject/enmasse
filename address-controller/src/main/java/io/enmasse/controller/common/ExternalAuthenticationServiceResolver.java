/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.common;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceResolver;

import java.util.Optional;

public class ExternalAuthenticationServiceResolver implements AuthenticationServiceResolver {

    @Override
    public String getHost(AuthenticationService authService) {
        return (String) authService.getDetails().get("host");
    }

    @Override
    public int getPort(AuthenticationService authService) {
        return (Integer) authService.getDetails().get("port");
    }

    @Override
    public Optional<String> getCaSecretName(AuthenticationService authService) {
        return Optional.ofNullable((String) authService.getDetails().get("caCertSecretName"));
    }

    @Override
    public Optional<String> getClientSecretName(AuthenticationService authService) {
        return Optional.ofNullable((String) authService.getDetails().get("clientCertSecretName"));
    }

    @Override
    public Optional<String> getSaslInitHost(String addressSpaceName, AuthenticationService authService) {
        return Optional.ofNullable((String) authService.getDetails().get("saslInitHost"));
    }
}
