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
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressStatusForwarder;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.address.model.MessageRedelivery;
import io.enmasse.config.LabelKeys;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.broker.ArtemisUtils;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientReceiver;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientSender;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.fail;

public class AddressUtils {
    private static Logger log = CustomLogger.getLogger();

    //TODO make this configurable via env var
    private static boolean verboseLogs = true;

    private AddressUtils() {
        //utility class no need to instantiate it
    }

    public static void disableVerboseLogs() {
        verboseLogs = false;
    }

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

    public static void delete(Address... destinations) {
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
        if (verboseLogs) {
            log.info("Creating addresses {}", new Object[]{addresses});
        }
        String operationID = TimeMeasuringSystem.startOperation(addresses.length > 0 ? SystemtestsOperation.CREATE_ADDRESS : SystemtestsOperation.DELETE_ADDRESS);
        if (verboseLogs) {
            log.info("Remove addresses in every addresses's address space");
        }
        for (Address address : addresses) {
            Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).withName(address.getMetadata().getName()).cascading(true).delete();
        }
        for (Address address : addresses) {
            address = Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).create(address);
            if (verboseLogs) {
                log.info("Address {} created", address.getMetadata().getName());
            }
        }
        if (wait) {
            waitForDestinationsReady(budget, addresses);
        }

        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void appendAddresses(TimeoutBudget budget, boolean wait, Address... addresses) throws Exception {
        if (verboseLogs) {
            log.info("Appending addresses {}", new Object[]{addresses});
        }
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.APPEND_ADDRESS);
        for (Address address : addresses) {
            address = Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).create(address);
            if (verboseLogs) {
                log.info("Address {} created", address.getMetadata().getName());
            }
        }
        if (wait) {
            waitForDestinationsReady(budget, addresses);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }


    public static void replaceAddress(Address destination, boolean wait, TimeoutBudget timeoutBudget) throws Exception {
        log.info("Replacing address {}", destination);
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

    public static boolean isAddressReady(AddressSpace addressSpace, Address address) {
        for (Address currentAdd : getAddresses(addressSpace)) {
            if (currentAdd.getMetadata().getName().equals(address.getMetadata().getName())
                    && currentAdd.getStatus().isReady()) {
                return true;
            }
        }
        return false;
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
        int expectedForwarders = address.getSpec().getForwarders().size();
        return expectedForwarders == address.getStatus().getForwarders().size() && address.getStatus().getForwarders().stream()
                .allMatch(AddressStatusForwarder::isReady);
    }

    private static FilterWatchListMultiDeletable<Address, AddressList> getAddressClient(Address... destinations) {
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

    public static void waitForDestinationsReady(Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        AddressUtils.waitForDestinationsReady(budget, destinations);
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

    private static void waitForAddressesMatched(TimeoutBudget timeoutBudget, int totalDestinations, FilterWatchListMultiDeletable<Address, AddressList> addressClient, AddressListMatcher addressListMatcher) {
        TestUtils.waitUntilCondition(totalDestinations + " match", phase -> {
            try {
                List<Address> addressList = addressClient.list().getItems();
                Map<String, Address> notMatched = addressListMatcher.matchAddresses(addressList);
                if (verboseLogs) {
                    notMatched.values().forEach(address ->
                        log.info("Waiting until address {} ready, message {}", address.getMetadata().getName(), address.getStatus().getMessages()));
                }
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
                return !addressesInSameAddrSpace.contains(address);
            } catch (KubernetesClientException e) {
                log.warn("Client can't read address resources");
                return false;
            }
        }, timeoutBudget);
    }

    public static String getTopicPrefix(AbstractClient clientEngine) {
        return (clientEngine instanceof ProtonJMSClientReceiver || clientEngine instanceof ProtonJMSClientSender) &&
                !(clientEngine instanceof PahoMQTTClientReceiver || clientEngine instanceof PahoMQTTClientSender) ? "topic://" : "";
    }

    interface AddressListMatcher {
        Map<String, Address> matchAddresses(List<Address> addressList);
    }

    public static List<Address> getAllStandardAddresses(AddressSpace addressspace) {
        return Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-queue-sharded"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue-sharded")
                        .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-topic-sharded"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic-sharded")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-anycast"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("anycast")
                        .withAddress("test-anycast")
                        .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-multicast"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("multicast")
                        .withAddress("test-multicast")
                        .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                        .endSpec()
                        .build());
    }

    public static List<Address> getAllBrokeredAddresses(AddressSpace addressspace) {
        return Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.BROKERED_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.BROKERED_TOPIC)
                        .endSpec()
                        .build());
    }

    public static void assertRedeliveryStatus(Address addr, MessageRedelivery expectedRedelivery) {
        Address reread = IsolatedResourcesManager.getInstance().getAddress(addr.getMetadata().getNamespace(), addr);
        if (expectedRedelivery == null) {
            assertThat(reread.getStatus().getMessageRedelivery(), nullValue());
        } else {
            assertThat(reread.getStatus().getMessageRedelivery(), notNullValue());

            if (expectedRedelivery.getMaximumDeliveryAttempts() != null) {
                assertThat(reread.getStatus().getMessageRedelivery().getMaximumDeliveryAttempts(), is(expectedRedelivery.getMaximumDeliveryAttempts()));
            } else {
                assertThat(reread.getStatus().getMessageRedelivery().getMaximumDeliveryAttempts(), nullValue());
            }

            if (expectedRedelivery.getRedeliveryDelayMultiplier() != null) {
                assertThat(reread.getStatus().getMessageRedelivery().getRedeliveryDelayMultiplier(), is(expectedRedelivery.getRedeliveryDelayMultiplier()));
            } else {
                assertThat(reread.getStatus().getMessageRedelivery().getRedeliveryDelayMultiplier(), nullValue());
            }

            if (expectedRedelivery.getRedeliveryDelay() != null) {
                assertThat(reread.getStatus().getMessageRedelivery().getRedeliveryDelay(), is(expectedRedelivery.getRedeliveryDelay()));
            } else {
                assertThat(reread.getStatus().getMessageRedelivery().getRedeliveryDelay(), nullValue());
            }

            if (expectedRedelivery.getMaximumDeliveryDelay() != null) {
                assertThat(reread.getStatus().getMessageRedelivery().getMaximumDeliveryDelay(), is(expectedRedelivery.getMaximumDeliveryDelay()));
            } else {
                assertThat(reread.getStatus().getMessageRedelivery().getMaximumDeliveryDelay(), nullValue());
            }
        }
    }



    // It'd be better if the address's status reflected when the expiry/address settings spaces were applied
    // but with our current architecture, agent (for the standard case) doesn't write the address status.
    // For now peep at the broker(s)
    public static void awaitAddressSettingsSync(AddressSpace addressSpace, Address addr, MessageRedelivery messageRedelivery) {
        Address reread = IsolatedResourcesManager.getInstance().getAddress(addr.getMetadata().getNamespace(), addr);
        MessageRedelivery expectedRedelivery =(messageRedelivery == null ? (reread.getStatus().getMessageRedelivery() == null ? new MessageRedelivery() : reread.getStatus().getMessageRedelivery()) : messageRedelivery);

        UserCredentials supportCredentials = ArtemisUtils.getSupportCredentials(addressSpace);
        List<String> brokers = new ArrayList<>();
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
            brokers.addAll(reread.getStatus().getBrokerStatuses().stream().map(BrokerStatus::getContainerId).collect(Collectors.toSet()));
            assertThat(brokers.size(), greaterThanOrEqualTo(1));
        } else {
            Map<String, String> brokerLabels = new HashMap<>();
            brokerLabels.put(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
            brokerLabels.put(LabelKeys.ROLE, "broker");

            List<Pod> brokerPods = Kubernetes.getInstance().listPods(brokerLabels);
            assertThat(brokerPods.size(), equalTo(1));
            brokers.add(brokerPods.get(0).getMetadata().getName());
        }

        brokers.forEach(name -> TestUtils.waitUntilCondition(() -> {
                    Map<String, Object> actualSettings = ArtemisUtils.getAddressSettings(Kubernetes.getInstance(), name, supportCredentials, reread.getSpec().getAddress());
                    int maxDeliveryAttempts = ((Number) actualSettings.get("maxDeliveryAttempts")).intValue();
                    String deadletter = String.valueOf(actualSettings.get("DLA"));
                    String expiry = String.valueOf(actualSettings.get("expiryAddress"));
                    boolean b = maxDeliveryAttemptsEquals(maxDeliveryAttempts, expectedRedelivery.getMaximumDeliveryAttempts());
                    if (!b) {
                        log.info("Address {} on broker {} does not have expected redelivery values: {}, dead letter: {}, expiry: {}." +
                                        " Actual maxDeliveryAttempts: {}, dead letter: {} expiry: {}",
                                reread.getMetadata().getName(), name,
                                expectedRedelivery, reread.getSpec().getDeadletter(), reread.getSpec().getExpiry(),
                                maxDeliveryAttempts, deadletter, expiry);
                    }
                    return b;
                }, Duration.ofMinutes(2), Duration.ofSeconds(5),
                () -> fail("Failed to await address settings to sync")));
    }

    private static boolean maxDeliveryAttemptsEquals(int artemisMaxDeliveryAttempts, Integer maximumDeliveryAttempts) {
        return Objects.equals(maximumDeliveryAttempts == null ? -1 : maximumDeliveryAttempts, artemisMaxDeliveryAttempts);
    }
}
