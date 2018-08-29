/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

public class CertComponent {
    private final String name;
    private final String uuid;
    private final String secretName;

    CertComponent(String name, String uuid, String secretName) {
        this.name = name;
        this.uuid = uuid;
        this.secretName = secretName;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSecretName() {
        return secretName;
    }

    @Override
    public String toString() {
        return "{name=" + name + "," +
                "uuid=" + uuid + "," +
                "secretName=" + secretName + "}";
    }
}
