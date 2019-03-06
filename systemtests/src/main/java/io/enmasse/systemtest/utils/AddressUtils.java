/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.DestinationPlan;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class AddressUtils {
    private static final String QUEUE = "queue";
    private static final String TOPIC = "topic";
    private static final String ANYCAST = "anycast";
    private static final String MULTICAST = "multicast";
    private static final String SUBSCRIPTION = "subscription";


    private static DoneableAddress createAddressResource(String name, String address, String type, String plan) {
        return new DoneableAddress(new AddressBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withAddress(sanitizeAddress(address))
                .withPlan(plan)
                .withType(type)
                .endSpec()
                .build());
    }

    public static Address createAddress(String name, String address, String type, String plan) {
        return createAddressResource(name, address, type, plan).done();
    }

    public static Address createAddress(String address, String type, String plan) {
        return createAddress(sanitizeAddress(address), address, type, plan);
    }

    public static Address createAddress(String name, String uuid, String addressSpace, String address, String type, String plan) {
        return createAddressResource(name, address, type, plan)
                .editMetadata()
                .withUid(uuid)
                .endMetadata()
                .editSpec()
                .withAddressSpace(addressSpace)
                .endSpec()
                .done();
    }

    public static Address createQueue(String name, String plan) {
        return createAddressResource(name, name, QUEUE, plan).done();
    }

    public static Address createTopic(String name, String plan) {
        return createAddressResource(name, name, TOPIC, plan).done();
    }

    public static Address createAnycast(String name, String plan) {
        return createAddressResource(name, name, ANYCAST, plan).done();
    }

    public static Address createMulticast(String name, String plan) {
        return createAddressResource(name, name, MULTICAST, plan).done();
    }

    public static Address createSubscription(String name, String topic, String plan) {
        return createAddressResource(name, name, SUBSCRIPTION, plan)
                .editSpec()
                .withTopic(topic)
                .endSpec()
                .done();
    }

    public static Address createMulticast(String name) {
        return createAddressResource(name, name, MULTICAST, DestinationPlan.STANDARD_SMALL_MULTICAST).done();
    }

    public static Address createAnycast(String name) {
        return createAddressResource(name, name, ANYCAST, DestinationPlan.STANDARD_SMALL_ANYCAST).done();
    }

    public static Address createSubscription(String name, String topic) {
        return createAddressResource(name, name, SUBSCRIPTION, DestinationPlan.STANDARD_SMALL_ANYCAST)
                .editSpec()
                .withTopic(topic)
                .endSpec()
                .done();
    }

    public static Address createAddress(AddressType type, String address, String plan, Optional<String> topic) {
        switch (type) {
            case QUEUE:
                return createQueue(address, plan);
            case TOPIC:
                return createTopic(address, plan);
            case ANYCAST:
                return createAnycast(address, plan);
            case MULTICAST:
                return createMulticast(address, plan);
            case SUBSCRIPTION:
                return createSubscription(address, topic.get(), plan);
            default:
                throw new IllegalStateException(String.format("Address type %s does not exists", type.toString()));
        }
    }

    public static JsonObject addressToJson(Address address) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(address));
    }

    public static JsonObject addressToJson(String addressSpace, Address address) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(new DoneableAddress(address)
                .editMetadata()
                .withName(generateAddressMetadataName(addressSpace, address))
                .endMetadata()
                .editSpec()
                .withAddressSpace(addressSpace)
                .endSpec()
                .done()));
    }

    public static String addressToYaml(Address address) throws Exception {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(addressToJson(address).toString());
        return new YAMLMapper().writeValueAsString(jsonNodeTree);
    }

    public static String generateAddressMetadataName(String addressSpace, Address address) {
        return address.getMetadata().getName().startsWith(addressSpace) ? address.getMetadata().getName() : String.format("%s.%s", addressSpace, sanitizeAddress(address.getMetadata().getName()));
    }

    public static String getQualifiedSubscriptionAddress(Address address) {
        return address.getSpec().getTopic() == null ? address.getSpec().getAddress() : address.getSpec().getTopic() + "::" + address;
    }

    public static String sanitizeAddress(String address) {
        return address != null ? address.toLowerCase().replaceAll("[^a-z0-9.\\-]", "") : address;
    }
}
