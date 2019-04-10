/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import java.util.Base64;

public class CertBundle {

    private String caCert;
    private String key;
    private String cert;

    public CertBundle(String caCert, String key, String cert) {
        this.caCert = caCert;
        this.key = key;
        this.cert = cert;
    }

    public String getCaCert() {
        return caCert;
    }

    public String getKey() {
        return key;
    }

    public String getCert() {
        return cert;
    }

    public String getCertB64() {
        return Base64.getEncoder().encodeToString(cert.getBytes());
    }

    public String getKeyB64() {
        return Base64.getEncoder().encodeToString(key.getBytes());
    }


}
