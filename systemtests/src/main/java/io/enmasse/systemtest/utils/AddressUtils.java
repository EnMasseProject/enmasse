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
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.timemeasuring.Operation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Predicate;

public class AddressUtils {
    private static final String QUEUE = "queue";
    private static final String TOPIC = "topic";
    private static final String ANYCAST = "anycast";
    private static final String MULTICAST = "multicast";
    private static final String SUBSCRIPTION = "subscription";
    private static Logger log = CustomLogger.getLogger();

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

    public static Address createAddressObject(String name, String address, String type, String plan) {
        return createAddressResource(name, address, type, plan).done();
    }

    public static Address createAddressObject(String address, String type, String plan) {
        return createAddressObject(sanitizeAddress(address), address, type, plan);
    }

    public static Address createAddressObject(String name, String uuid, String addressSpace, String address, String type, String plan) {
        return createAddressResource(name, address, type, plan)
                .editMetadata()
                .withUid(uuid)
                .endMetadata()
                .editSpec()
                .withAddressSpace(addressSpace)
                .endSpec()
                .done();
    }

    public static Address createQueueAddressObject(String name, String plan) {
        return createAddressResource(name, name, QUEUE, plan).done();
    }

    public static Address createTopicAddressObject(String name, String plan) {
        return createAddressResource(name, name, TOPIC, plan).done();
    }

    public static Address createAnycastAddressObject(String name, String plan) {
        return createAddressResource(name, name, ANYCAST, plan).done();
    }

    public static Address createMulticastAddressObject(String name, String plan) {
        return createAddressResource(name, name, MULTICAST, plan).done();
    }

    public static Address createSubscriptionAddressObject(String name, String topic, String plan) {
        return createAddressResource(name, name, SUBSCRIPTION, plan)
                .editSpec()
                .withTopic(topic)
                .endSpec()
                .done();
    }

    public static Address createMulticastAddressObject(String name) {
        return createAddressResource(name, name, MULTICAST, DestinationPlan.STANDARD_SMALL_MULTICAST).done();
    }

    public static Address createAnycastAddressObject(String name) {
        return createAddressResource(name, name, ANYCAST, DestinationPlan.STANDARD_SMALL_ANYCAST).done();
    }

    public static Address createSubscriptionAddressObject(String name, String topic) {
        return createAddressResource(name, name, SUBSCRIPTION, DestinationPlan.STANDARD_SMALL_ANYCAST)
                .editSpec()
                .withTopic(topic)
                .endSpec()
                .done();
    }

    public static Address createAddressObject(AddressType type, String address, String plan, Optional<String> topic) {
        switch (type) {
            case QUEUE:
                return createQueueAddressObject(address, plan);
            case TOPIC:
                return createTopicAddressObject(address, plan);
            case ANYCAST:
                return createAnycastAddressObject(address, plan);
            case MULTICAST:
                return createMulticastAddressObject(address, plan);
            case SUBSCRIPTION:
                return createSubscriptionAddressObject(address, topic.get(), plan);
            default:
                throw new IllegalStateException(String.format("Address type %s does not exists", type.toString()));
        }
    }

    public static JsonObject addressToJson(Address address) throws Exception {
        return addressToJson(address.getSpec().getAddressSpace(), address);
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

    public static Address jsonToAddress(JsonObject addressJsonObject) throws Exception {
        log.info("Got address object: {}", addressJsonObject.toString());
        return new ObjectMapper().readValue(addressJsonObject.toString(), Address.class);
    }

    public static String addressToYaml(Address address) throws Exception {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(addressToJson(address).toString());
        return new YAMLMapper().writeValueAsString(jsonNodeTree);
    }

    public static String addressToYaml(String addressSpace, Address address) throws Exception {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(addressToJson(addressSpace, address).toString());
        return new YAMLMapper().writeValueAsString(jsonNodeTree);
    }

    public static String generateAddressMetadataName(String addressSpace, Address address) {
        return address.getMetadata().getName().startsWith(addressSpace) ? address.getMetadata().getName() : String.format("%s.%s", addressSpace, sanitizeAddress(address.getMetadata().getName()));
    }

    public static String getQualifiedSubscriptionAddress(Address address) {
        return address.getSpec().getTopic() == null ? address.getSpec().getAddress() : address.getSpec().getTopic() + "::" + address.getSpec().getAddress();
    }

    public static String sanitizeAddress(String address) {
        return address != null ? address.toLowerCase().replaceAll("[^a-z0-9.\\-]", "") : address;
    }

    public static void delete(AddressApiClient apiClient, AddressSpace addressSpace, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(Operation.DELETE_ADDRESS);
        apiClient.deleteAddresses(addressSpace, destinations);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void setAddresses(AddressApiClient apiClient, Kubernetes kubernetes, TimeoutBudget budget, AddressSpace addressSpace, boolean wait, int expectedCode, Address... addresses) throws Exception {
        log.info("Addresses {} in address space {} will be created", addresses, addressSpace.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(addresses.length > 0 ? Operation.CREATE_ADDRESS : Operation.DELETE_ADDRESS);
        apiClient.setAddresses(addressSpace, expectedCode, addresses);
        if (wait) {
            if (addressSpace.getSpec().getType().equals("standard")) {
                if (addresses.length == 0) {
                    TestUtils.waitForExpectedReadyPods(kubernetes, addressSpace, kubernetes.getExpectedPods(addressSpace.getSpec().getPlan()), budget);
                }
            }
            TestUtils.waitForDestinationsReady(apiClient, addressSpace, budget, addresses);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void setAddresses(AddressApiClient apiClient, Kubernetes kubernetes, TimeoutBudget budget, AddressSpace addressSpace, boolean wait, Address... addresses) throws Exception {
        setAddresses(apiClient, kubernetes, budget, addressSpace, wait, HttpURLConnection.HTTP_CREATED, addresses);
    }

    public static void appendAddresses(AddressApiClient apiClient, Kubernetes kubernetes, TimeoutBudget budget, AddressSpace addressSpace, boolean wait, Address... destinations) throws Exception {
        log.info("Addresses {} in address space {} will be updated", destinations, addressSpace.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(Operation.APPEND_ADDRESS);
        apiClient.appendAddresses(addressSpace, destinations);
        if (wait) {
            if (addressSpace.getSpec().getType().toString().equals("standard")) {
                if (destinations.length == 0) {
                    TestUtils.waitForExpectedReadyPods(kubernetes, addressSpace, kubernetes.getExpectedPods(addressSpace.getSpec().getPlan()), budget);
                }
            }
            TestUtils.waitForDestinationsReady(apiClient, addressSpace, budget, destinations);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }


    public static void replaceAddress(AddressApiClient addressApiClient, AddressSpace addressSpace, Address destination, boolean wait, TimeoutBudget timeoutBudget) throws Exception {
        log.info("Address {} in address space {} will be replaced", destination, addressSpace.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(Operation.UPDATE_ADDRESS);
        addressApiClient.replaceAddress(addressSpace.getMetadata().getName(), destination, 200);
        if (wait) {
            TestUtils.waitForDestinationsReady(addressApiClient, addressSpace, timeoutBudget, destination);
            TestUtils.waitForDestinationPlanApplied(addressApiClient, addressSpace, timeoutBudget, destination);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void appendAddresses(AddressApiClient apiClient, Kubernetes kubernetes, TimeoutBudget budget, AddressSpace addressSpace, boolean wait, int batchSize, io.enmasse.address.model.Address... destinations) throws Exception {
        log.info("Addresses {} in address space {} will be updated", destinations, addressSpace.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(Operation.APPEND_ADDRESS);
        apiClient.appendAddresses(addressSpace, batchSize, destinations);
        if (wait) {
            if (addressSpace.getSpec().getType().toString().equals("standard")) {
                if (destinations.length == 0) {
                    TestUtils.waitForExpectedReadyPods(kubernetes, addressSpace, kubernetes.getExpectedPods(addressSpace.getSpec().getPlan()), budget);
                }
            }
            TestUtils.waitForDestinationsReady(apiClient, addressSpace, budget, destinations);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static <T> List<T> convertToListAddress(JsonObject htmlResponse, Class<T> clazz, Predicate<JsonObject> filter) throws Exception {
        if (htmlResponse != null) {
            String kind = htmlResponse.getString("kind");
            List<T> addresses = new ArrayList<>();
            switch (kind) {
                case "Address":
                    if (filter.test(htmlResponse)) {
                        if (clazz.equals(String.class)) {
                            addresses.add(clazz.cast(htmlResponse.getJsonObject("spec").getString("address")));
                        } else if (clazz.equals(Address.class)) {
                            addresses.add(clazz.cast(AddressUtils.jsonToAddress(htmlResponse)));
                        }
                    }
                    break;
                case "AddressList":
                    JsonArray items = htmlResponse.getJsonArray("items");
                    if (items != null) {
                        for (int i = 0; i < items.size(); i++) {
                            if (filter.test(items.getJsonObject(i))) {
                                if (clazz.equals(String.class)) {
                                    addresses.add(clazz.cast(items.getJsonObject(i).getJsonObject("spec").getString("address")));
                                } else if (clazz.equals(Address.class)) {
                                    addresses.add(clazz.cast(AddressUtils.jsonToAddress(items.getJsonObject(i))));
                                }
                            }
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unknown kind: '%s'", kind));
            }
            return addresses;
        }
        throw new IllegalArgumentException("htmlResponse can't be null");
    }


    public static Future<List<String>> getAddresses(AddressApiClient apiClient, AddressSpace addressSpace,
                                                    Optional<String> addressName, List<String> skipAddresses) throws Exception {
        JsonObject response = apiClient.getAddresses(addressSpace, addressName);
        CompletableFuture<List<String>> listOfAddresses = new CompletableFuture<>();
        listOfAddresses.complete(convertToListAddress(response, String.class, object -> !skipAddresses.contains(object.getJsonObject("spec").getString("address"))));
        return listOfAddresses;
    }

    public static Future<List<Address>> getAddressesObjects(AddressApiClient apiClient, AddressSpace addressSpace,
                                                            Optional<String> addressName, Optional<HashMap<String, String>> queryParams,
                                                            List<String> skipAddresses) throws Exception {
        JsonObject response = apiClient.getAddresses(addressSpace, addressName, queryParams);
        CompletableFuture<List<Address>> listOfAddresses = new CompletableFuture<>();
        listOfAddresses.complete(convertToListAddress(response, Address.class, object -> !skipAddresses.contains(object.getJsonObject("spec").getString("address"))));
        return listOfAddresses;
    }

    public static Future<List<Address>> getAllAddressesObjects(AddressApiClient apiClient) throws Exception {
        JsonObject response = apiClient.getAllAddresses();
        CompletableFuture<List<Address>> listOfAddresses = new CompletableFuture<>();
        listOfAddresses.complete(convertToListAddress(response, Address.class, x -> true));
        return listOfAddresses;
    }
}
