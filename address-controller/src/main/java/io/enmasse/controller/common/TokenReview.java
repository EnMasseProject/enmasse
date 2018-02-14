/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

public class TokenReview {
    private final String userName;
    private final boolean isAuthenticated;

    public TokenReview(String userName, boolean isAuthenticated) {
        this.userName = userName;
        this.isAuthenticated = isAuthenticated;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getUserName() {
        return userName;
    }
}
