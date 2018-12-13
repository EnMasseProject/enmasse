/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

/**
 * Queue and its address
 */
public class QueueInfo {
    private final String address;
    private final String queueName;

    public QueueInfo(String address, String queueName) {
        this.address = address;
        this.queueName = queueName;
    }

    public String getAddress() {
        return address;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getQualifiedAddress() {
        return address + "::" + queueName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueueInfo queueInfo = (QueueInfo) o;

        if (!address.equals(queueInfo.address)) return false;
        return queueName.equals(queueInfo.queueName);
    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + queueName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "{address=" + address + ", queue=" + queueName + "}";
    }
}
