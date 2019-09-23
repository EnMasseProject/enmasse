/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.certs;

public class BrokerCertBundle {

    String caCert;
    byte[] keystore;
    byte[] truststore;

    public BrokerCertBundle(String caCert, byte[] keystore, byte[] truststore) {
        this.caCert = caCert;
        this.keystore = keystore;
        this.truststore = truststore;
    }

    public String getCaCert() {
        return caCert;
    }

    public void setCaCert(String caCert) {
        this.caCert = caCert;
    }

    public byte[] getKeystore() {
        return keystore;
    }

    public void setKeystore(byte[] keystore) {
        this.keystore = keystore;
    }

    public byte[] getTruststore() {
        return truststore;
    }

    public void setTruststore(byte[] truststore) {
        this.truststore = truststore;
    }

}
