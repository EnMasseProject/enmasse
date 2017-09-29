/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
