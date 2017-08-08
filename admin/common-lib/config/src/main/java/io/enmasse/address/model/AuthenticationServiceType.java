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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The type of authentication services supported in EnMasse.
 */
public enum AuthenticationServiceType {
    NONE(Collections.emptyMap()),
    STANDARD(Collections.emptyMap()),
    EXTERNAL(new HashMap<String, Class>(){{
        put("host", String.class);
        put("port", Integer.class);
        put("caCertSecretName", String.class);
        put("clientCertSecretName", String.class);
        put("saslInitHost", String.class);
    }});

    private final Map<String, Class> detailsFields;

    AuthenticationServiceType(Map<String, Class> detailsFields) {
        this.detailsFields = Collections.unmodifiableMap(detailsFields);
    }

    public Map<String, Class> getDetailsFields() {
        return detailsFields;
    }

    public String getName() {
        return name().toLowerCase();
    }

    public static AuthenticationServiceType create(String name) {
        return valueOf(name.toUpperCase());
    }
}
