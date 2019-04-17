/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TokenReview {
    private final String userName;
    private final String userId;
    private final List<String> groups;
    private final Map<String, List<String>> extra;

    private final boolean isAuthenticated;

    public TokenReview(String userName, String userId, List<String> groups, Map<String, List<String>> extra, boolean isAuthenticated) {
        this.userName = userName;
        this.userId = userId;
        this.groups = groups;
        this.extra = extra;
        this.isAuthenticated = isAuthenticated;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public List<String> getGroups() {
        return groups;
    }

    public Map<String, List<String>> getExtra() {
        return extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenReview that = (TokenReview) o;
        return isAuthenticated == that.isAuthenticated &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(groups, that.groups) &&
                Objects.equals(extra, that.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, userId, groups, extra, isAuthenticated);
    }
}
