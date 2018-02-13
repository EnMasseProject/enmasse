/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import java.net.PasswordAuthentication;
import java.util.Arrays;

public class SingleUserAuthenticator implements UserAuthenticator {
    private PasswordAuthentication passwordAuthentication;

    public SingleUserAuthenticator(PasswordAuthentication passwordAuthentication) {
        this.passwordAuthentication = passwordAuthentication;
    }

    @Override
    public boolean authenticate(String username, String password) {
        return passwordAuthentication.getUserName().equals(username)
                && Arrays.equals(passwordAuthentication.getPassword(), password.toCharArray());
    }
}
