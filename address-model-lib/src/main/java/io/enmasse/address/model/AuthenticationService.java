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
