/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class CertSpec {
    private final String provider;
    private final String secretName;
    private final String tlsKey;
    private final String tlsCert;

    public CertSpec(String provider, String secretName, String tlsKey, String tlsCert) {
        this.provider = provider;
        this.secretName = secretName;
        this.tlsKey = tlsKey;
        this.tlsCert = tlsCert;
    }

    public String getProvider() {
        return provider;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getTlsKey() {
        return tlsKey;
    }

    public String getTlsCert() {
        return tlsCert;
    }

    public static class Builder {
        private String provider;
        private String secretName;
        private String tlsKey;
        private String tlsCert;

        public Builder() {
        }

        public Builder(CertSpec certSpec) {
            this.provider = certSpec.provider;
            this.secretName = certSpec.secretName;
            this.tlsKey = certSpec.tlsKey;
            this.tlsCert = certSpec.tlsCert;
        }

        public String getProvider() {
            return provider;
        }

        public String getSecretName() {
            return secretName;
        }

        public Builder setProvider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder setSecretName(String secretName) {
            this.secretName = secretName;
            return this;
        }

        public Builder setTlsKey(String tlsKey) {
            this.tlsKey = tlsKey;
            return this;
        }

        public Builder setTlsCert(String tlsCert) {
            this.tlsCert = tlsCert;
            return this;
        }

        public CertSpec build() {
            return new CertSpec(provider, secretName, tlsKey, tlsCert);
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
