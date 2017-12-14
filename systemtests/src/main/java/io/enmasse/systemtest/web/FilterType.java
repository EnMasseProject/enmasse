package io.enmasse.systemtest.web;


public enum FilterType {
    TYPE, NAME, CONTAINER, HOSTNAME, USER, ENCRYPTED;

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}
