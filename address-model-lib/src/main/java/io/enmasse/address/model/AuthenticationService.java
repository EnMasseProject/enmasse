/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an authentication service for an {@link AddressSpace}.
 */
public class AuthenticationService {
    private final AuthenticationServiceType type;
    private final Map<String, Object> details;

    private AuthenticationService(AuthenticationServiceType type, Map<String, Object> details) {
        this.type = type;
        this.details = details;
    }

    public AuthenticationServiceType getType() {
        return type;
    }

    public Map<String, Object> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    public static class Builder {
        private AuthenticationServiceType type = AuthenticationServiceType.NONE;
        private Map<String, Object> details = new HashMap<>();

        public Builder setType(AuthenticationServiceType type) {
            this.type = type;
            return this;
        }

        public Builder setDetails(Map<String, Object> details) {
            Objects.requireNonNull(details);
            this.details = new HashMap<>(details);
            return this;
        }

        public AuthenticationService build() {
            Objects.requireNonNull(type);
            Objects.requireNonNull(details);
            return new AuthenticationService(type, details);
        }
    }
}
