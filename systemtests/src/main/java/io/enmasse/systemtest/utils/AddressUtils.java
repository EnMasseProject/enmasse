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
import io.enmasse.address.model.AddressStatus;
import io.enmasse.address.model.AddressStatusForwarder;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
        for (Address address : client.list().getItems()) {
            client.withName(address.getMetadata().getName()).cascading(true).delete();
            waitForAddressDeleted(address, new TimeoutBudget(5, TimeUnit.MINUTES));
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void setAddresses(TimeoutBudget budget, boolean wait, Address... addresses) throws Exception {
        log.info("Addresses {} will be created", new Object[]{addresses});
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
        log.info("Addresses {} will be appended", new Object[]{addresses});
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
        Address existing = client.withName(destination.getMetadata().getName()).get();
        existing.setSpec(destination.getSpec());
        client.withName(existing.getMetadata().getName()).patch(existing);
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

    public static boolean areForwardersReady(Address address) {
        return Optional.ofNullable(address)
                .map(Address::getStatus)
                .map(AddressStatus::getForwarders)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .flatMap(Collection::stream)
                .map(AddressStatusForwarder::isReady)
                .allMatch(ready -> ready == true);
    }

    private static FilterWatchListMultiDeletable<Address, AddressList, Boolean, Watch, Watcher<Address>> getAddressClient(Address... destinations) {
        List<String> namespaces = Stream.of(destinations)
                .map(address -> address.getMetadata().getNamespace())
                .distinct()
                .collect(Collectors.toList());
        if (namespaces.size() != 1) {
            return Kubernetes.getInstance().getAddressClient().inAnyNamespace();
        } else {
            return Kubernetes.getInstance().getAddressClient(namespaces.get(0));
        }
    }

    public static void waitForDestinationsReady(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_READY);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::isAddressReady, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForDestinationPlanApplied(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_PLAN_CHANGE);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::isPlanSynced, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForBrokersDrained(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_BROKER_DRAINED);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::areBrokersDrained, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForForwardersReady(TimeoutBudget budget, Address... destinations) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.ADDRESS_WAIT_FORWARDERS);
        waitForAddressesMatched(budget, destinations.length, getAddressClient(destinations), addressList -> checkAddressesMatching(addressList, AddressUtils::areForwardersReady, destinations));
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void waitForAddressesMatched(TimeoutBudget timeoutBudget, int totalDestinations, FilterWatchListMultiDeletable<Address, AddressList, Boolean, Watch, Watcher<Address>> addressClient, AddressListMatcher addressListMatcher) throws Exception {
        TestUtils.waitUntilCondition(totalDestinations + " match", phase -> {
            try {
                List<Address> addressList = addressClient.list().getItems();
                Map<String, Address> notMatched = addressListMatcher.matchAddresses(addressList);
                notMatched.values().forEach(address ->
                        log.info("Waiting until address {} ready, message {}", address.getMetadata().getName(), address.getStatus().getMessages()));
                if (!notMatched.isEmpty() && phase == WaitPhase.LAST_TRY) {
                    log.info(notMatched.size() + " out of " + totalDestinations + " addresses are not matched: " + notMatched.values());
                }
                return notMatched.isEmpty();
            } catch (KubernetesClientException e) {
                if (phase == WaitPhase.LAST_TRY) {
                    log.error("Client can't read address resources", e);
                } else {
                    log.warn("Client can't read address resources");
                }
                return false;
            }
        }, timeoutBudget);
    }

    private static Map<String, Address> checkAddressesMatching(List<Address> addressList, Predicate<Address> predicate, Address... destinations) {
        Map<String, Address> notMatchingAddresses = new HashMap<>();
        for (Address destination : destinations) {
            Optional<Address> lookupAddressResult = addressList.stream()
                    .filter(addr -> addr.getMetadata().getName().contains(destination.getMetadata().getName()))
                    .findFirst();
            if (lookupAddressResult.isEmpty()) {
                notMatchingAddresses.put(destination.getSpec().getAddress(), null);
            } else if (!predicate.test(lookupAddressResult.get())) {
                notMatchingAddresses.put(destination.getSpec().getAddress(), lookupAddressResult.get());
            }
        }
        return notMatchingAddresses;
    }

    public static void waitForAddressDeleted(Address address, TimeoutBudget timeoutBudget) throws Exception {
        Kubernetes kubernetes = Kubernetes.getInstance();

        TestUtils.waitUntilCondition(address + " match", phase -> {
            try {
                AddressList addressList = kubernetes.getAddressClient().inNamespace(address.getMetadata().getNamespace()).list();
                List<Address> addressesInSameAddrSpace = addressList.getItems().stream()
                        .filter(address1 -> Address.extractAddressSpace(address1)
                                .equals(Address.extractAddressSpace(address))).collect(Collectors.toList());
                if (!addressesInSameAddrSpace.contains(address)) {
                    return true;
                }
                return false;
            } catch (KubernetesClientException e) {
                log.warn("Client can't read address resources");
                return false;
            }
        }, timeoutBudget);
    }

    public static String getTopicPrefix(boolean topicSwitch) {
        return topicSwitch ? "topic://" : "";
    }

    interface AddressListMatcher {
        Map<String, Address> matchAddresses(List<Address> addressList);
    }
}
