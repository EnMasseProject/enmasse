package io.enmasse.systemtest.web;


public enum FilterType {
    TYPE, NAME;

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}
