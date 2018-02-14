/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class CertProvider {
    private final String name;
    private final String secretName;

    public CertProvider(String name, String secretName) {
        this.name = name;
        this.secretName = secretName;
    }

    public String getName() {
        return name;
    }

    public String getSecretName() {
        return secretName;
    }
}
