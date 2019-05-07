/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.enmasse.address.model.*;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
                .withName(sanitizeAddress(name))
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

    public static List<Address> getAddresses(AddressSpace addressSpace) {
        return Kubernetes.getInstance().getAddressClient().inAnyNamespace().list().getItems().stream()
                .filter(address -> address.getMetadata().getName().split("\\.")[0].equals(addressSpace.getMetadata().getName())).collect(Collectors.toList());
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
        if (address.getMetadata().getName() == null) {
            return null;
        }
        return address.getMetadata().getName().startsWith(addressSpace) ? address.getMetadata().getName() : String.format("%s.%s", addressSpace, sanitizeAddress(address.getMetadata().getName()));
    }

    public static String getQualifiedSubscriptionAddress(Address address) {
        return address.getSpec().getTopic() == null ? address.getSpec().getAddress() : address.getSpec().getTopic() + "::" + address.getSpec().getAddress();
    }

    public static String sanitizeAddress(String address) {
        return address != null ? address.toLowerCase().replaceAll("[^a-z0-9.\\-]", "") : address;
    }

    public static void delete(Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS);
        Arrays.stream(destinations).forEach(address -> Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).delete(address));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void setAddresses(TimeoutBudget budget, AddressSpace addressSpace, boolean wait, Address... addresses) throws Exception {
        var client = Kubernetes.getInstance().getAddressClient(addressSpace.getMetadata().getNamespace());
        log.info("Addresses {} in address space {} will be created", addresses, addressSpace.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(addresses.length > 0 ? SystemtestsOperation.CREATE_ADDRESS : SystemtestsOperation.DELETE_ADDRESS);
        client.delete(getAddresses(addressSpace));
        for (Address address : addresses) {
            address = client.create(new DoneableAddress(address).editMetadata().withName(addressSpace.getMetadata().getName() + "." + address.getSpec().getAddress()).endMetadata().done());
            log.info("Address {} created", address.getMetadata().getName());
        }
        if (wait) {
            waitForDestinationsReady(addressSpace, budget, addresses);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void appendAddresses(TimeoutBudget budget, AddressSpace addressSpace, boolean wait, Address... addresses) throws Exception {
        var client = Kubernetes.getInstance().getAddressClient(addressSpace.getMetadata().getNamespace());
        log.info("Addresses {} in address space {} will be updated", addresses, addressSpace.getMetadata().getName());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.APPEND_ADDRESS);
        for (Address address : addresses) {
            address = client.create(new DoneableAddress(address).editMetadata().withName(addressSpace.getMetadata().getName() + "." + address.getSpec().getAddress()).endMetadata().done());
            log.info("Address {} created", address.getMetadata().getName());
        }
        if (wait) {
            waitForDestinationsReady(addressSpace, budget, addresses);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }


    public static void replaceAddress(AddressSpace addressSpace, Address destination, boolean wait, TimeoutBudget timeoutBudget) throws Exception {
        log.info("Address {} in address space {} will be replaced", destination, addressSpace.getMetadata().getName());
        var client = Kubernetes.getInstance().getAddressClient();
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.UPDATE_ADDRESS);
        client.createOrReplace(new DoneableAddress(destination)
                .editMetadata()
                .withName(addressSpace.getMetadata().getName() + "." + destination.getSpec().getAddress())
                .endMetadata()
                .done());
        if (wait) {
            waitForDestinationsReady(addressSpace, timeoutBudget, destination);
            waitForDestinationPlanApplied(addressSpace, timeoutBudget, destination);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }


    public static boolean isAddressReady(Address address) {
        return address.getStatus().isReady();
    }

    public static boolean isPlanSynced(Address address) {
        boolean isReady = false;
        Map<String, String> annotations = address.getMetadata().getAnnotations();
        if (annotations != null) {
            String appliedPlan = address.getStatus().getPlanStatus().getName();
            String actualPlan = address.getSpec().getPlan();
            isReady = actualPlan.equals(appliedPlan);
        }
        return isReady;
    }

    public static boolean areBrokersDrained(Address address) {
        boolean isReady = true;
        List<BrokerStatus> brokerStatuses = address.getStatus().getBrokerStatuses();
        for (BrokerStatus status : brokerStatuses) {
            if (BrokerState.Draining.equals(status.getState())) {
                isReady = false;
                break;
            }
        }
        return isReady;
    }

    interface AddressListMatcher {
        Map<String, Address> matchAddresses(List<Address> addressList);
    }

    public static void waitForDestinationsReady(AddressSpace addressSpace, TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_READY);
        waitForAddressesMatched(addressSpace, budget, destinations.length, addressList -> checkAddressesMatching(addressList, AddressUtils::isAddressReady, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForDestinationPlanApplied(AddressSpace addressSpace, TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_PLAN_CHANGE);
        waitForAddressesMatched(addressSpace, budget, destinations.length, addressList -> checkAddressesMatching(addressList, AddressUtils::isPlanSynced, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForBrokersDrained(AddressSpace addressSpace, TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_BROKER_DRAINED);
        waitForAddressesMatched(addressSpace, budget, destinations.length, addressList -> checkAddressesMatching(addressList, AddressUtils::areBrokersDrained, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void waitForAddressesMatched(AddressSpace addressSpace, TimeoutBudget timeoutBudget, int totalDestinations, AddressListMatcher addressListMatcher) throws Exception {
        Map<String, Address> notMatched = new HashMap<>();

        while (timeoutBudget.timeLeft() >= 0) {
            List<Address> addressList = getAddresses(addressSpace);
            notMatched = addressListMatcher.matchAddresses(addressList);
            log.info("Waiting until addresses ready: {}", notMatched.values().stream().map(address -> address.getMetadata().getName()).collect(Collectors.toList()));
            if (notMatched.isEmpty()) {
                Thread.sleep(5000); //TODO: remove this sleep after fix for ready check will be available
                break;
            }
            Thread.sleep(5000);
        }

        if (!notMatched.isEmpty()) {
            List<Address> addressList = getAddresses(addressSpace);
            notMatched = addressListMatcher.matchAddresses(addressList);
            throw new IllegalStateException(notMatched.size() + " out of " + totalDestinations + " addresses are not matched: " + notMatched.values());
        }
    }

    private static Address lookupAddress(List<Address> addressList, String address) {
        for (Address addr : addressList) {
            if (addr.getSpec().getAddress().equals(address)) {
                return addr;
            }
        }
        return null;
    }

    private static Map<String, Address> checkAddressesMatching(List<Address> addressList, Predicate<Address> predicate, Address... destinations) {
        Map<String, Address> notMatchingAddresses = new HashMap<>();
        for (Address destination : destinations) {
            Address addressObject = lookupAddress(addressList, destination.getSpec().getAddress());
            if (addressObject == null) {
                notMatchingAddresses.put(destination.getSpec().getAddress(), null);
            } else if (!predicate.test(addressObject)) {
                notMatchingAddresses.put(destination.getSpec().getAddress(), addressObject);
            }
        }
        return notMatchingAddresses;
    }

    public static String getTopicPrefix(boolean topicSwitch) {
        return topicSwitch ? "topic://" : "";
    }
}
