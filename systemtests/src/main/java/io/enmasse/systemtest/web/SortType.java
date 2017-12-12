package io.enmasse.systemtest.web;

public enum SortType {
    NAME,
    SENDERS,
    RECEIVERS;

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}
