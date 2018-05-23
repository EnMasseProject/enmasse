/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import io.enmasse.api.auth.AuthApi;

class AuthWrapper {
    private final AuthApi authApi;
    private final boolean enableRbac;
    private final boolean enableUserLookup;

    public AuthWrapper(AuthApi authApi, boolean enableRbac, boolean enableUserLookup) {
        this.authApi = authApi;
        this.enableRbac = enableRbac;
        this.enableUserLookup = enableUserLookup;
    }

    public boolean isRbacEnabled() {
        return enableRbac;
    }

    public boolean isEnableUserLookup() {
        return enableUserLookup;
    }

    public AuthApi getAuthApi() {
        return authApi;
    }
}
