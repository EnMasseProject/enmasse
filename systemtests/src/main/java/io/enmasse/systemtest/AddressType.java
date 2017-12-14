package io.enmasse.systemtest;


public enum AddressType {
    QUEUE, TOPIC, MULTICAST, ANYCAST;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}


