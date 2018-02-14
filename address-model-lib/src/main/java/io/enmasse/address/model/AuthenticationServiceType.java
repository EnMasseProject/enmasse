/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

/**
 * The type of authentication services supported in EnMasse.
 */
public enum AuthenticationServiceType {
    NONE,
    STANDARD,
    EXTERNAL(new AuthenticationServiceDetail("host", String.class, true),
        new AuthenticationServiceDetail("port", Integer.class, true),
            new AuthenticationServiceDetail("caCertSecretName", String.class, true),
            new AuthenticationServiceDetail("clientCertSecretName", String.class, true),
            new AuthenticationServiceDetail("saslInitHost", String.class, true));

    private final Map<String, Class> detailsFields = new HashMap<>();
    private final Set<String> mandatoryFields = new HashSet<>();

    AuthenticationServiceType(AuthenticationServiceDetail ... details) {
        for (AuthenticationServiceDetail detail : details) {
            detailsFields.put(detail.getName(), detail.getType());

            if (detail.isMandatory()) {
                mandatoryFields.add(detail.getName());
            }
        }
    }

    public Map<String, Class> getDetailsFields() {
        return Collections.unmodifiableMap(detailsFields);
    }

    public Set<String> getMandatoryFields() {
        return Collections.unmodifiableSet(mandatoryFields);
    }


    public String getName() {
        return name().toLowerCase();
    }

    public static AuthenticationServiceType create(String name) {
        return valueOf(name.toUpperCase());
    }
}
