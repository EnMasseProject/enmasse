/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Objects;

public class IdentityProviderParams {

    public static final IdentityProviderParams NULL_PARAMS = new IdentityProviderParams(null, null, null);

    private final String identityProviderUrl;
    private final String identityProviderClientId;
    private final String identityProviderClientSecret;

    public IdentityProviderParams(String identityProviderUrl, String identityProviderClientId, String identityProviderClientSecret) {
        this.identityProviderUrl = identityProviderUrl;
        this.identityProviderClientId = identityProviderClientId;
        this.identityProviderClientSecret = identityProviderClientSecret;
    }

    public static IdentityProviderParams fromKube(KubernetesClient client, String configName) {

        ConfigMap config = client.configMaps().withName(configName).get();
        String identityProviderUrl = config.getData().get("identityProviderUrl");
        String identityProviderClientId = config.getData().get("identityProviderClientId");
        String identityProviderClientSecret = config.getData().get("identityProviderClientSecret");

        return new IdentityProviderParams(identityProviderUrl, identityProviderClientId, identityProviderClientSecret);
    }

    @Override
    public String toString() {
        return "{identityProviderUrl=" + identityProviderUrl + "," +
                "identityProviderClientId=" + identityProviderClientId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityProviderParams that = (IdentityProviderParams) o;
        return Objects.equals(identityProviderUrl, that.identityProviderUrl) &&
                Objects.equals(identityProviderClientId, that.identityProviderClientId) &&
                Objects.equals(identityProviderClientSecret, that.identityProviderClientSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderUrl, identityProviderClientId, identityProviderClientSecret);
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
}
