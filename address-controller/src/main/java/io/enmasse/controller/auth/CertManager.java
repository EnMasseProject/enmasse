/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.util.Collection;

/**
 * Interface for certificate managers
 */
public interface CertManager {
    void issueRouteCert(String secretName, String namespace, String... hostnames) throws Exception;

    Collection<CertComponent> listComponents(String namespace);
    boolean certExists(CertComponent component);
    boolean certExists(String name);
    CertSigningRequest createCsr(CertComponent component);
    Cert signCsr(CertSigningRequest request, String secretName);
    void createSecret(Cert cert, final String caSecretName);

    void createSelfSignedCertSecret(String secretName);
}
