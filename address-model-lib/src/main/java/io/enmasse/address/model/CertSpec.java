/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class CertSpec {
    private String provider;
    private String secretName;

    public CertSpec() { }

    public CertSpec(String provider) {
        this.provider = provider;
    }

    public CertSpec(String provider, String secretName) {
        this.provider = provider;
        this.secretName = secretName;
    }

    public String getProvider() {
        return provider;
    }

    public CertSpec setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public String getSecretName() {
        return secretName;
    }

    public CertSpec setSecretName(String secretName) {
        this.secretName = secretName;
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{provider=").append(provider).append(",")
                .append("secretName").append(secretName).append("}")
                .toString();
    }
}
