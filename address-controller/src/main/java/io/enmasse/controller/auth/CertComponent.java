/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

public class CertComponent {
    private final String name;
    private final String namespace;
    private final String secretName;

    CertComponent(String name, String namespace, String secretName) {
        this.name = name;
        this.namespace = namespace;
        this.secretName = secretName;
    }

    public String getName() {
        return name;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "{name=" + name + "," +
                "secretName=" + secretName + "," +
                "namespace=" + namespace + "}";
    }
}
