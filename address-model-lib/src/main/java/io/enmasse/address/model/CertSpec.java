/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class CertSpec {
    private final String provider;
    private final String secretName;

    public CertSpec(String provider, String secretName) {
        this.provider = provider;
        this.secretName = secretName;
    }

    public String getProvider() {
        return provider;
    }

    public String getSecretName() {
        return secretName;
    }

    public static class Builder {
        private String provider;
        private String secretName;

        public Builder() {
        }

        public Builder(CertSpec certSpec) {
            this.provider = certSpec.provider;
            this.secretName = certSpec.secretName;
        }

        public String getProvider() {
            return provider;
        }

        public String getSecretName() {
            return secretName;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public void setSecretName(String secretName) {
            this.secretName = secretName;
        }

        public CertSpec build() {
            return new CertSpec(provider, secretName);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{provider=").append(provider).append(",")
                .append("secretName").append(secretName).append("}")
                .toString();
    }
}
