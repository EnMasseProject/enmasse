/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import java.util.Set;

class UserDataImpl implements UserData {

    private final String userId;
    private final String userName;
    private final Set<String> groups;

    public UserDataImpl(String userId, String userName, Set<String> groups) {
        this.userId = userId;
        this.userName = userName;
        this.groups = groups;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public Set<String> getGroups() {
        return groups;
    }
}
