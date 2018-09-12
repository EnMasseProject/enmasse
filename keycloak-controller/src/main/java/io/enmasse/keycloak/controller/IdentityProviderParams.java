/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;
import java.util.Optional;

public class IdentityProviderParams {

    private String identityProviderUrl;
    private String identityProviderClientId;
    private String identityProviderClientSecret;

    public static IdentityProviderParams fromKube(KubernetesClient client, String configName) {
        IdentityProviderParams params = new IdentityProviderParams();

        ConfigMap config = client.configMaps().withName(configName).get();
        getEnv(config.getData(), "identityProviderUrl").ifPresent(params::setIdentityProviderUrl);
        getEnv(config.getData(), "identityProviderClientId").ifPresent(params::setIdentityProviderClientId);
        getEnv(config.getData(), "identityProviderClientSecret").ifPresent(params::setIdentityProviderClientSecret);

        return params;
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    @Override
    public String toString() {
        return "{identityProviderUrl=" + identityProviderUrl + "," +
                "identityProviderClientId=" + identityProviderClientId + "}";
    }

    public String getIdentityProviderUrl() {
        return identityProviderUrl;
    }

    public String getIdentityProviderClientId() {
        return identityProviderClientId;
    }

    public String getIdentityProviderClientSecret() {
        return identityProviderClientSecret;
    }

    public void setIdentityProviderUrl(String identityProviderUrl) {
        this.identityProviderUrl = identityProviderUrl;
    }

    public void setIdentityProviderClientId(String identityProviderClientId) {
        this.identityProviderClientId = identityProviderClientId;
    }

    public void setIdentityProviderClientSecret(String identityProviderClientSecret) {
        this.identityProviderClientSecret = identityProviderClientSecret;
    }
}
