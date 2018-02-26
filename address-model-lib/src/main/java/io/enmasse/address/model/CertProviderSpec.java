/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.Map;

public class CertProviderSpec {
    private final String name;
    private final String secretName;
    private final Map<String, String> parameters;

    public CertProviderSpec(String name, String secretName, Map<String, String> parameters) {
        this.name = name;
        this.secretName = secretName;
        this.parameters = parameters;
    }

    public CertProviderSpec(String name, String secretName) {
        this(name, secretName, Collections.emptyMap());
    }

    public String getName() {
        return name;
    }

    public String getSecretName() {
        return secretName;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name=").append(name).append(",")
                .append("secretName").append(secretName).append("}")
                .toString();
    }
}
