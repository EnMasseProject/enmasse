/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
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

    @Override
    public String toString() {
        return "{" + "type=" + type.toString() + "}";
    }
}
