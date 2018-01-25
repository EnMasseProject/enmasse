package io.enmasse.systemtest;

public enum Group {
    MANAGE,
    SEND_ALL,
    RECV_ALL,
    MONITOR;

    @Override
    public String toString() {
        return super.toString().toLowerCase().replace("_all", "_*");
    }
}
