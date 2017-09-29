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
