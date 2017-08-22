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
