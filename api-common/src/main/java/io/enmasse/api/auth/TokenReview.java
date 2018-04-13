/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

public class TokenReview {
    private final String userName;
    private final String userId;
    private final boolean isAuthenticated;

    public TokenReview(String userName, String userId, boolean isAuthenticated) {
        this.userName = userName;
        this.userId = userId;
        this.isAuthenticated = isAuthenticated;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }
}
