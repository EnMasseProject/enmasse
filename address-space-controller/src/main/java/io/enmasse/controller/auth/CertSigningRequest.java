/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.io.File;

public class CertSigningRequest {
    private final CertComponent certComponent;
    private final File csrFile;
    private final File keyFile;

    CertSigningRequest(CertComponent certComponent, File csrFile, File keyFile) {
        this.certComponent = certComponent;
        this.csrFile = csrFile;
        this.keyFile = keyFile;
    }

    public CertComponent getCertComponent() {
        return certComponent;
    }

    public File getCsrFile() {
        return csrFile;
    }

    public File getKeyFile() {
        return keyFile;
    }
}
