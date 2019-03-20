/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.io.File;

public class CertBundle {

    private File caCert;
    private File crtFile;
    private String key;
    private String cert;

    public CertBundle(File caCert, File crtFile, String key, String cert) {
        this.caCert = caCert;
        this.crtFile = crtFile;
        this.key = key;
        this.cert = cert;
    }

    public File getCaCert() {
        return caCert;
    }

    public File getCrtFile() {
        return crtFile;
    }

    public String getKey() {
        return key;
    }

    public String getCert() {
        return cert;
    }

}
