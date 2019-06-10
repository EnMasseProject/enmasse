/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddressUtils {
    private static Logger log = CustomLogger.getLogger();

    public static List<Address> getAddresses(AddressSpace addressSpace) {
        return getAddresses(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
    }

    public static List<Address> getAddresses(String namespace, String addressSpace) {
        return Kubernetes.getInstance().getAddressClient(namespace).list().getItems().stream()
                .filter(address -> getAddressSpaceNameFromAddress(address).equals(addressSpace)).collect(Collectors.toList());
    }

    public static String getAddressSpaceNameFromAddress(Address address) {
        return address.getMetadata().getName().split("\\.")[0];
    }

    public static JsonObject addressToJson(Address address) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(address));
    }

    public static String addressToYaml(Address address) throws Exception {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(addressToJson(address).toString());
        return new YAMLMapper().writeValueAsString(jsonNodeTree);
    }

    public static String generateAddressMetadataName(AddressSpace addressSpace, String address) {
        return String.format("%s.%s", addressSpace.getMetadata().getName(), sanitizeAddress(address));
    }

    public static String getQualifiedSubscriptionAddress(Address address) {
        return address.getSpec().getTopic() == null ? address.getSpec().getAddress() : address.getSpec().getTopic() + "::" + address.getSpec().getAddress();
    }

    public static String sanitizeAddress(String address) {
        return address != null ? address.toLowerCase().replaceAll("[^a-z0-9.\\-]", "") : address;
    }

    public static void delete(Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS);
        Arrays.stream(destinations).forEach(address -> Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).cascading(true).delete());
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void delete(AddressSpace addressSpace) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS);
        var client = Kubernetes.getInstance().getAddressClient(addressSpace.getMetadata().getNamespace());
        getAddresses(addressSpace).forEach(addr -> client.withName(addr.getMetadata().getName()).cascading(true).delete());
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void setAddresses(TimeoutBudget budget, boolean wait, Address... addresses) throws Exception {
        log.info("Addresses {} will be created", addresses);
        String operationID = TimeMeasuringSystem.startOperation(addresses.length > 0 ? SystemtestsOperation.CREATE_ADDRESS : SystemtestsOperation.DELETE_ADDRESS);
        log.info("Remove addresses in every addresses's address space");
        for (Address address : addresses) {
            Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).cascading(true).delete();
        }
        for (Address address : addresses) {
            address = Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).create(address);
            log.info("Address {} created", address.getMetadata().getName());
        }
        if (wait) {
            waitForDestinationsReady(budget, addresses);
        }

        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void appendAddresses(TimeoutBudget budget, boolean wait, Address... addresses) throws Exception {
        log.info("Addresses {} will be appended", addresses);
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.APPEND_ADDRESS);
        for (Address address : addresses) {
            address = Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).create(address);
            log.info("Address {} created", address.getMetadata().getName());
        }
        if (wait) {
            waitForDestinationsReady(budget, addresses);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }


    public static void replaceAddress(Address destination, boolean wait, TimeoutBudget timeoutBudget) throws Exception {
        log.info("Address {} will be replaced", destination);
        var client = Kubernetes.getInstance().getAddressClient(destination.getMetadata().getNamespace());
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.UPDATE_ADDRESS);
        client.createOrReplace(destination);
        Thread.sleep(10_000);
        if (wait) {
            waitForDestinationsReady(timeoutBudget, destination);
            waitForDestinationPlanApplied(timeoutBudget, destination);
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

    private static FilterWatchListMultiDeletable<Address, AddressList, Boolean, Watch, Watcher<Address>>  getAddressClient(Address... destinations) {
        List<String> namespaces = Stream.of(destinations)
                .map(Address::getMetadata)
                .map(ObjectMeta::getNamespace)
                .distinct()
                .collect(Collectors.toList());
        if(namespaces.isEmpty() || namespaces.size()>1) {
            return Kubernetes.getInstance().getAddressClient().inAnyNamespace();
        } else {
            return Kubernetes.getInstance().getAddressClient(namespaces.get(0));
        }
    }

    public static void waitForDestinationsReady(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_READY);
        Thread.sleep(2000);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::isAddressReady, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForDestinationPlanApplied(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_PLAN_CHANGE);
        Thread.sleep(2000);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::isPlanSynced, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForBrokersDrained(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_BROKER_DRAINED);
        Thread.sleep(2000);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::areBrokersDrained, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void waitForAddressesMatched(TimeoutBudget timeoutBudget, int totalDestinations, FilterWatchListMultiDeletable<Address, AddressList, Boolean, Watch, Watcher<Address>> addressClient, AddressListMatcher addressListMatcher) throws Exception {
        Map<String, Address> notMatched = new HashMap<>();

        while (timeoutBudget.timeLeft() >= 0) {
            List<Address> addressList = addressClient.list().getItems();
            notMatched = addressListMatcher.matchAddresses(addressList);
            notMatched.values().forEach(address ->
                    log.info("Waiting until address {} ready, message {}", address.getMetadata().getName(), address.getStatus().getMessages()));
            if (notMatched.isEmpty()) {
                break;
            }
            Thread.sleep(5000);
        }

        if (!notMatched.isEmpty()) {
            List<Address> addressList = addressClient.list().getItems();
            notMatched = addressListMatcher.matchAddresses(addressList);
            throw new IllegalStateException(notMatched.size() + " out of " + totalDestinations + " addresses are not matched: " + notMatched.values());
        }
    }

    private static Map<String, Address> checkAddressesMatching(List<Address> addressList, Predicate<Address> predicate, Address... destinations) {
        Map<String, Address> notMatchingAddresses = new HashMap<>();
        for (Address destination : destinations) {
            Optional<Address> lookupAddressResult = addressList.stream()
                    .filter(addr -> addr.getSpec().getAddress().equals(destination.getSpec().getAddress()))
                    .findFirst();
            if (lookupAddressResult.isEmpty()) {
                notMatchingAddresses.put(destination.getSpec().getAddress(), null);
            } else if (!predicate.test(lookupAddressResult.get())) {
                notMatchingAddresses.put(destination.getSpec().getAddress(), lookupAddressResult.get());
            }
        }
        return notMatchingAddresses;
    }

    public static String getTopicPrefix(boolean topicSwitch) {
        return topicSwitch ? "topic://" : "";
    }
}
