package io.enmasse.systemtest;

public enum Group {
    MANAGE,
    SEND_ALL,
    RECV_ALL,
    MONITOR,
    VIEW_CONSOLE;

    @Override
    public String toString() {
        return super.toString().toLowerCase().replace("_all", "_*");
    }
}
