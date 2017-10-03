/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return host;
    }

    @Override
    public int getPort(AuthenticationService authService) {
        return port;
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
