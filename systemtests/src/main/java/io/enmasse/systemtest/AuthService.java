package io.enmasse.systemtest;

public enum AuthService {
    NONE, STANDARD;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
