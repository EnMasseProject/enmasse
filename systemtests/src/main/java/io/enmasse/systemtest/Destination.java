/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class Destination {
    public static final String QUEUE = "queue";
    public static final String TOPIC = "topic";
    public static final String ANYCAST = "anycast";
    public static final String MULTICAST = "multicast";
    public static final String SUBSCRIPTION = "subscription";
    private final String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(type, that.type) &&
                Objects.equals(plan, that.plan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, type, plan);
    }

    private final String address;
    private final String type;
    private final String plan;
    private final String uuid;
    private final String addressSpace;
    private final String topic;

    public Destination(String name, String address, String type, String plan) {
        this(name, address, type, plan, Optional.empty());
    }

    public Destination(String address, String type, String plan) {
        this(TestUtils.sanitizeAddress(address), address, type, plan);
    }

    public Destination(String name, String uuid, String addressSpace, String address, String type, String plan) {
        this.name = TestUtils.sanitizeAddress(name);
        this.address = address;
        this.type = type;
        this.plan = plan;
        this.uuid = uuid;
        this.addressSpace = addressSpace;
        this.topic = null;
    }

    public Destination(String address, String type, String plan, Optional<String> topic) {
        this(TestUtils.sanitizeAddress(address), address, type, plan, topic);
    }

    public Destination(String name, String address, String type, String plan, Optional<String> topic) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.plan = plan;
        this.uuid = null;
        this.addressSpace = null;
        this.topic = topic.orElse(null);
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

    public static Destination subscription(String address, String topic, String plan) {
        return new Destination(address, SUBSCRIPTION, plan, Optional.of(topic));
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

    public String getTopic() {
        return topic;
    }

    public String getQualifiedSubscriptionAddress() {
        return topic == null ? address : topic + "::" + address;
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
    public String toString() {
        return "{name=" + name + ", address=" + address + "}";
    }


    public JsonObject toJson(String version) {
        return toJson(version, this.addressSpace);
    }

    public JsonObject toJson(String version, String addressSpace) {
        JsonObject entry = new JsonObject();
        entry.put("apiVersion", version);
        entry.put("kind", "Address");
        entry.put("metadata", this.jsonMetadata(addressSpace));
        entry.put("spec", this.jsonSpec());
        return entry;
    }

    public String getAddressName(String addressSpace) {
        return this.getName().startsWith(addressSpace) ? this.getName() : String.format("%s.%s", addressSpace, this.getName());
    }

    public JsonObject jsonMetadata() {
        return jsonMetadata(addressSpace);
    }

    public JsonObject jsonMetadata(String addressSpace) {
        JsonObject metadata = new JsonObject();
        if (this.getName() != null) {
            metadata.put("name", getAddressName(addressSpace));
        }
        if (this.getUuid() != null) {
            metadata.put("uid", this.getUuid());
        }
        if (this.getAddressSpace() != null) {
            metadata.put("addressSpace", this.getAddressSpace());
        } else if (addressSpace != null) {
            metadata.put("addressSpace", addressSpace);
        }
        return metadata;
    }


    public JsonObject jsonSpec() {
        JsonObject spec = new JsonObject();
        if (this.getAddress() != null) {
            spec.put("address", this.getAddress());
        }
        if (this.getType() != null) {
            spec.put("type", this.getType());
        }
        if (this.getPlan() != null) {
            spec.put("plan", this.getPlan());
        }
        if (this.topic != null) {
            spec.put("topic", this.topic);
        }
        return spec;
    }

    public String toYaml(String version) throws IOException {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(this.toJson(version).toString());
        return new YAMLMapper().writeValueAsString(jsonNodeTree);
    }

}
