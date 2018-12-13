/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static io.enmasse.user.model.v1.UserAuthenticationType.federated;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAuthentication {
    private final UserAuthenticationType type;

    // For password type auth
    private final String password;

    // For federated identity auth
    private final String provider;

    // For federated identity auth
    private final String federatedUserid;
    private final String federatedUsername;

    public UserAuthentication(@JsonProperty("type") UserAuthenticationType type,
                              @JsonProperty("password") String password,
                              @JsonProperty("provider") String provider,
                              @JsonProperty("federatedUserid") String federatedUserid,
                              @JsonProperty("federatedUsername") String federatedUsername) {
        this.type = type;
        this.password = password;
        this.provider = provider;
        this.federatedUserid = federatedUserid;
        this.federatedUsername = federatedUsername;
    }

    public UserAuthenticationType getType() {
        return type;
    }

    public String getPassword() {
        return password;
    }

    public String getProvider() {
        return provider;
    }

    public String getFederatedUserid() {
        return federatedUserid;
    }

    public String getFederatedUsername() {
        return federatedUsername;
    }

    public void validate() {
        Objects.requireNonNull(type, "'type' must be set");
    }

    public static class Builder {
        private UserAuthenticationType type;
        private String password;
        private String provider;
        private String federatedUserid;
        private String federatedUsername;

        public Builder setType(UserAuthenticationType type) {
            this.type = type;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setProvider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder setFederatedUserid(String federatedUserid) {
            this.federatedUserid = federatedUserid;
            return this;
        }

        public Builder setFederatedUsername(String federatedUsername) {
            this.federatedUsername = federatedUsername;
            return this;
        }

        public UserAuthentication build() {
            Objects.requireNonNull(type);
            return new UserAuthentication(type, password, provider, federatedUserid, federatedUsername);
        }
    }
}
