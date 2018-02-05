package io.enmasse.systemtest.selenium;

public enum SortType {
    NAME,
    SENDERS,
    RECEIVERS,
    HOSTNAME,
    CONTAINER_ID;

    @Override
    public String toString() {
        return super.toString().replace('_', ' ').toUpperCase();
    }
}
