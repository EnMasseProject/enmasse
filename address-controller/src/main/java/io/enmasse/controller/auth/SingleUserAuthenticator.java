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
