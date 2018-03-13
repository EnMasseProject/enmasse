/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

public class Destination {
    private static final String QUEUE = "queue";
    private static final String TOPIC = "topic";
    private static final String ANYCAST = "anycast";
    private static final String MULTICAST = "multicast";
    private final String name;
    private final String address;
    private final String type;
    private final String plan;
    private final String uuid;
    private final String addressSpace;

    public Destination(String name, String address, String type, String plan) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.plan = plan;
        this.uuid = null;
        this.addressSpace = null;
    }

    public Destination(String address, String type, String plan) {
        this(TestUtils.sanitizeAddress(address), address, type, plan);
    }

    public Destination(String name, String uuid, String addressSpace, String address, String type, String plan) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.plan = plan;
        this.uuid = uuid;
        this.addressSpace = addressSpace;
    }

    public static Destination queue(String address, String plan) {
        return new Destination(address, QUEUE, plan);
    }

    public static Destination topic(String address, String plan) {
        return new Destination(address, TOPIC, plan);
    }

    public static Destination anycast(String address) {
        return new Destination(address, ANYCAST, "standard-anycast");
    }

    public static Destination multicast(String address) {
        return new Destination(address, MULTICAST, "standard-multicast");
    }

    public static Destination anycast(String address, String plan) {
        return new Destination(address, ANYCAST, plan);
    }

    public static Destination multicast(String address, String plan) {
        return new Destination(address, MULTICAST, plan);
    }

    public static boolean isQueue(Destination d) {
        return QUEUE.equals(d.type);
    }

    public static boolean isTopic(Destination d) {
        return TOPIC.equals(d.type);
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPlan() {
        return plan;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAddressSpace() {
        return addressSpace;
    }

    /**
     * The concept of a group id is still used to wait for the
     * necessary broker to be available. This is usually the address
     * except for pooled queues in which case it is the name of the
     * plan.
     */
    public String getDeployment() {
        if (plan.startsWith("pooled")) {
            return "broker";
        } else {
            return address;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Destination that = (Destination) o;

        if (!type.equals(that.type)) return false;
        if (!address.equals(that.address)) return false;
        return plan.equals(that.plan);

    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + plan.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return address;
    }
}
