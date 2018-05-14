/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.fabric8.kubernetes.api.model.Secret;

import java.util.Collection;

/**
 * Interface for certificate managers
 */
public interface CertManager {
    Collection<CertComponent> listComponents(String namespace);
    boolean certExists(CertComponent component);
    Secret getCertSecret(String namespace, String name);
    CertSigningRequest createCsr(CertComponent component);
    Cert signCsr(CertSigningRequest request, Secret secret);
    void createSecret(Cert cert, final Secret caSecret);

    Secret createSelfSignedCertSecret(String namespace, String secretName);

    void grantServiceAccountAccess(Secret secret, String saName, String saNamespace);
}
