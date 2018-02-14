/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.io.File;

public class Cert {
    private final CertComponent component;
    private final File keyFile;
    private final File certFile;

    Cert(CertComponent component, File keyFile, File certFile) {
        this.component = component;
        this.keyFile = keyFile;
        this.certFile = certFile;
    }

    public CertComponent getComponent() {
        return component;
    }

    public File getKeyFile() {
        return keyFile;
    }

    public File getCertFile() {
        return certFile;
    }

    @Override
    public String toString() {
        return component.toString();
    }
}
