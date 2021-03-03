/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.sundr.builder.annotations.Buildable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSpec extends AbstractWithAdditionalProperties {
    private final static Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9]+([a-z0-9_@.:\\-]*[a-z0-9]+|[a-z0-9]*)$");


    private String username;
    private UserAuthentication authentication;
    private List<UserAuthorization> authorization;

    // Works around builder problem
    public UserSpec() {
        this(null, null, null);
    }

    @JsonCreator
    public UserSpec(@JsonProperty("username") String username,
                    @JsonProperty("authentication") UserAuthentication authentication,
                    @JsonProperty("authorization") List<UserAuthorization> authorization) {
        this.username = username;
        this.authentication = authentication;
        this.authorization = authorization;
    }

    public String getUsername() {
        return username;
    }

    public UserAuthentication getAuthentication() {
        return authentication;
    }

    public List<UserAuthorization> getAuthorization() {
        return authorization;
    }

    public void validate() {
        Objects.requireNonNull(username, "'username' must be set");
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new UserValidationFailedException("Invalid username '" + username + "', must match " + USERNAME_PATTERN.toString());
        }

        if (authentication != null) {
            authentication.validate();
        }

        if (authorization != null) {
            for (UserAuthorization authz : authorization) {
                authz.validate();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("username=").append(username).append(",");
        sb.append("authentication=").append(authentication).append(",");
        sb.append("authorization=").append(authorization);
        sb.append("}");
        return sb.toString();
    }
}
