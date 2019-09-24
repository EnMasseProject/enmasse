/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.certs;

public class BrokerCertBundle {

    byte[] caCert;
    byte[] keystore;
    byte[] truststore;
    byte[] clientCert;
    byte[] clientKey;

    public BrokerCertBundle(byte[] caCert, byte[] keystore, byte[] truststore, byte[] clientCert, byte[] clientKey) {
        this.caCert = caCert;
        this.keystore = keystore;
        this.truststore = truststore;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
    }

    public byte[] getCaCert() {
        return caCert;
    }

    public void setCaCert(byte[] caCert) {
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

    public byte[] getClientCert() {
        return clientCert;
    }

    public void setClientCert(byte[] clientCert) {
        this.clientCert = clientCert;
    }

    public byte[] getClientKey() {
        return clientKey;
    }

    public void setClientKey(byte[] clientKey) {
        this.clientKey = clientKey;
    }

}
