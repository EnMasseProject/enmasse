package io.enmasse.controller.auth;

public class AllowAllUserDatabase implements UserDatabase {
    @Override
    public boolean hasUser(String username) {
        return true;
    }

    @Override
    public void addUser(String username, String password) {

    }

    @Override
    public boolean authenticate(String username, String password) {
        return true;
    }
}
