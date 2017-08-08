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
package enmasse.controller.common;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceResolver;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Optional;

/**
 * Resolves the none authentication service host name.
 */
public class NoneAuthenticationServiceResolver implements AuthenticationServiceResolver {
    private final OpenShiftClient openShiftClient;
    private final Service service;

    public NoneAuthenticationServiceResolver(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
        this.service = openShiftClient.services().withName("none-authservice").get();
    }

    @Override
    public String getHost(AuthenticationService authService) {
        return service.getSpec().getClusterIP();
    }

    @Override
    public int getPort(AuthenticationService authService) {
        return service.getSpec().getPorts().get(0).getPort();
    }

    @Override
    public Optional<String> getCaSecretName(AuthenticationService authService) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getClientSecretName(AuthenticationService authService) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSaslInitHost(AuthenticationService authService) {
        return Optional.empty();
    }
}
