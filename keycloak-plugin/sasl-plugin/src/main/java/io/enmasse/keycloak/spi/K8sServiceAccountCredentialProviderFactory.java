/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.models.KeycloakSession;

public class K8sServiceAccountCredentialProviderFactory implements CredentialProviderFactory<PasswordCredentialProvider> {


    public static final String PROVIDER_ID="k8s-service-account";
    @Override
    public K8sServiceAccountCredentialProvider create(KeycloakSession session) {
        return new K8sServiceAccountCredentialProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }



}
