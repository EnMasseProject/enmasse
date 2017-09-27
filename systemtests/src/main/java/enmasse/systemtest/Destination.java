/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.systemtest;

import java.util.Optional;

public class Destination {
    private static final String QUEUE = "queue";
    private static final String TOPIC = "topic";
    private static final String ANYCAST = "anycast";
    private static final String MULTICAST = "multicast";
    private final String address;
    private final String type;
    private final Optional<String> plan;

    public Destination(String address, String type, Optional<String> plan) {
        this.address = address;
        this.type = type;
        this.plan = plan;
    }

    public static Destination queue(String address) {
        return queue(address, Optional.empty());
    }

    public static Destination queue(String address, Optional<String> plan) {
        return new Destination(address, QUEUE, plan);
    }

    public static Destination topic(String address) {
        return new Destination(address, TOPIC, Optional.empty());
    }

    public static Destination topic(String address, Optional<String> plan) {
        return new Destination(address, TOPIC, plan);
    }

    public static Destination anycast(String address) {
        return new Destination(address, ANYCAST, Optional.empty());
    }

    public static Destination multicast(String address) {
        return new Destination(address, MULTICAST, Optional.empty());
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

    public String getAddress() {
        return address;
    }

    public Optional<String> getPlan() {
        return plan;
    }

    /**
     * The concept of a group id is still used to wait for the
     * necessary broker to be available. This is usually the address
     * except for pooled queues in which case it is the name of the
     * plan.
     */
    public String getGroup() {
        if (isQueue(this) && plan.isPresent() && plan.get().startsWith("pooled")) {
            return plan.get();
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
