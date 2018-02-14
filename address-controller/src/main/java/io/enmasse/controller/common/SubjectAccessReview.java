/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

public class SubjectAccessReview {
    private final String user;
    private final boolean allowed;

    public SubjectAccessReview(String user, boolean allowed) {
        this.user = user;
        this.allowed = allowed;
    }

    public String getUser() {
        return user;
    }

    public boolean isAllowed() {
        return allowed;
    }
}
