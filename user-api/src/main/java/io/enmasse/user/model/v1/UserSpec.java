/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSpec {
    private final String username;
    private final UserAuthentication authentication;
    private final List<UserAuthorization> authorization;
    private final Pattern usernamePattern = Pattern.compile("^[a-z0-9]+([a-z0-9_\\-]*[a-z0-9]+|[a-z0-9]*)$");

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
        if (!usernamePattern.matcher(username).matches()) {
            throw new UserValidationFailedException("Invalid username '" + username + "', must match " + usernamePattern.toString());
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

    public static class Builder {
        private String username;
        private UserAuthentication authentication;
        private List<UserAuthorization> authorization;


        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setAuthentication(UserAuthentication authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder setAuthorization(List<UserAuthorization> authorization) {
            this.authorization = authorization;
            return this;
        }

        public UserSpec build() {
            Objects.requireNonNull(username);
            Objects.requireNonNull(authentication);
            return new UserSpec(username, authentication, authorization);
        }
    }
}
