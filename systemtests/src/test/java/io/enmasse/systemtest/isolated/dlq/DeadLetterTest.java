/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.dlq;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.address.model.MessageRedelivery;
import io.enmasse.address.model.MessageRedeliveryBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.config.LabelKeys;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.broker.ArtemisUtils;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(ISOLATED)
@Tag(ACCEPTANCE)
class DeadLetterTest extends TestBase implements ITestBaseIsolated {
    private static final Logger log = CustomLogger.getLogger();
    private static final Modified DELIVERY_FAILED = new Modified();
    {
        DELIVERY_FAILED.setDeliveryFailed(true);
    }

    @ParameterizedTest(name = "testAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressSpecified(String type) throws Exception {
        doTestMessageRedelivery(AddressSpaceType.getEnum(type), AddressType.QUEUE, null, new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build(),
                new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build());
    }

    @ParameterizedTest(name = "testAddressPlanSpecified-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressPlanSpecified(String type) throws Exception {
        doTestMessageRedelivery(AddressSpaceType.getEnum(type), AddressType.QUEUE, new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build(), null,
                new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build());
    }

    @ParameterizedTest(name = "testSubscriptionAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard"})
    void testSubscriptionAddressSpecified(String type) throws Exception {
        doTestMessageRedelivery(AddressSpaceType.getEnum(type), AddressType.TOPIC, null, new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build(),
                new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build());
    }

    private void doTestMessageRedelivery(AddressSpaceType addressSpaceType, AddressType addressType, MessageRedelivery addrPlanRedelivery, MessageRedelivery addrRedelivery, MessageRedelivery expectedRedelivery) throws Exception {
        final String infraConfigName = "redelivery-infra";
        final String spacePlanName = "space-plan-redelivery";
        final String addrPlanName = "addr-plan-redelivery";

        final String baseSpacePlan;
        final String baseAddressPlan;
        final String deadLetterAddressPlan;
        final long messageExpiryScanPeriod = 1000L;

        PodTemplateSpec brokerInfraTtlOverride = new PodTemplateSpecBuilder()
                .withNewSpec()
                .withInitContainers(new ContainerBuilder()
                        .withName("broker-plugin")
                        .withEnv(new EnvVar("MESSAGE_EXPIRY_SCAN_PERIOD", String.format("%d", messageExpiryScanPeriod), null)).build()).endSpec().build();

        if (AddressSpaceType.STANDARD == addressSpaceType) {
            baseSpacePlan =  AddressSpacePlans.STANDARD_SMALL;
            baseAddressPlan = addressType == AddressType.QUEUE ? DestinationPlan.STANDARD_MEDIUM_QUEUE : DestinationPlan.STANDARD_SMALL_TOPIC;
            deadLetterAddressPlan = DestinationPlan.STANDARD_DEADLETTER;
            StandardInfraConfig infraConfig = isolatedResourcesManager.getStandardInfraConfig("default");
            StandardInfraConfig ttlInfra = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withName(infraConfigName)
                    .endMetadata()
                    .withNewSpecLike(infraConfig.getSpec())
                    .withNewBrokerLike(infraConfig.getSpec().getBroker())
                    .withPodTemplate(brokerInfraTtlOverride)
                    .endBroker()
                    .endSpec()
                    .build();
            isolatedResourcesManager.createInfraConfig(ttlInfra);
        } else {
            baseSpacePlan =  AddressSpacePlans.BROKERED;
            baseAddressPlan = addressType == AddressType.QUEUE ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.BROKERED_TOPIC;
            deadLetterAddressPlan = DestinationPlan.BROKERED_DEADLETTER;
            BrokeredInfraConfig infraConfig = isolatedResourcesManager.getBrokeredInfraConfig("default");
            BrokeredInfraConfig ttlInfra = new BrokeredInfraConfigBuilder()
                    .withNewMetadata()
                    .withName(infraConfigName)
                    .endMetadata()
                    .withNewSpecLike(infraConfig.getSpec())
                    .withNewBrokerLike(infraConfig.getSpec().getBroker())
                    .withPodTemplate(brokerInfraTtlOverride)
                    .endBroker()
                    .endSpec()
                    .build();
            isolatedResourcesManager.createInfraConfig(ttlInfra);
        }

        AddressSpacePlan smallSpacePlan = kubernetes.getAddressSpacePlanClient().withName(baseSpacePlan).get();
        AddressPlan smallPlan = kubernetes.getAddressPlanClient().withName(baseAddressPlan).get();
        AddressPlan deadletterPlan = kubernetes.getAddressPlanClient().withName(deadLetterAddressPlan).get();

        AddressPlan addrPlan = new AddressPlanBuilder()
                .withNewMetadata()
                .withName(addrPlanName)
                .endMetadata()
                .withNewSpecLike(smallPlan.getSpec())
                .withMessageRedelivery(addrPlanRedelivery)
                .endSpec()
                .build();

        List<String> plans = new ArrayList<>(smallSpacePlan.getSpec().getAddressPlans());
        plans.add(addrPlan.getMetadata().getName());

        AddressSpacePlan spacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(spacePlanName)
                .endMetadata()
                .withNewSpecLike(smallSpacePlan.getSpec())
                .withAddressPlans(plans)
                .withInfraConfigRef(infraConfigName)
                .endSpec()
                .build();

        isolatedResourcesManager.createAddressPlan(addrPlan);
        isolatedResourcesManager.createAddressSpacePlan(spacePlan);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("message-ttl-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(spacePlan.getAddressSpaceType())
                .withPlan(spacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        Address deadletter = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "deadletter"))
                .endMetadata()
                .withNewSpec()
                .withType(deadletterPlan.getAddressType())
                .withPlan(deadletterPlan.getMetadata().getName())
                .withAddress("deadletter")
                .endSpec()
                .build();

        Address addr = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-redelivery"))
                .endMetadata()
                .withNewSpec()
                .withType(addressType.toString())
                .withMessageRedelivery(addressType == AddressType.TOPIC ? null : addrRedelivery)
                .withAddress("message-redelivery")
                .withDeadLetterAddress(addressType == AddressType.TOPIC ? null : deadletter.getSpec().getAddress())
                .withPlan(addrPlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        isolatedResourcesManager.setAddresses(deadletter, addr);

        Address recvAddr;
        if (addressType == AddressType.TOPIC && AddressSpaceType.STANDARD == addressSpaceType) {
                recvAddr = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(kubernetes.getInfraNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-redelivery-sub"))
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressType.SUBSCRIPTION.toString())
                        .withMessageRedelivery(addrRedelivery)
                        .withAddress("message-redelivery-sub")
                        .withDeadLetterAddress(deadletter.getSpec().getAddress())
                        .withTopic(addr.getSpec().getAddress())
                        .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                        .endSpec()
                        .build();
                isolatedResourcesManager.setAddresses(recvAddr);
        } else {
            recvAddr = addr;
        }

        assertRedeliveryStatus(recvAddr, expectedRedelivery);
        awaitAddressSettingsSync(addressSpace, recvAddr);

        UserCredentials user = new UserCredentials("user", "passwd");
        isolatedResourcesManager.createOrUpdateUser(addressSpace, user);

        sendAndReceiveFailingDeliveries(addressSpace, addr, recvAddr, user, List.of(createMessage(addr)));

        if (addrPlan.getSpec().getMessageRedelivery() != null) {
            isolatedResourcesManager.replaceAddressPlan(new AddressPlanBuilder()
                    .withMetadata(addrPlan.getMetadata())
                    .withNewSpecLike(addrPlan.getSpec())
                    .withMessageRedelivery(new MessageRedelivery())
                    .endSpec()
                    .build());
        }

        if (recvAddr.getSpec().getMessageRedelivery() != null || recvAddr.getSpec().getDeadLetterAddress() != null) {
            isolatedResourcesManager.replaceAddress(new AddressBuilder()
                    .withMetadata(recvAddr.getMetadata())
                    .withNewSpecLike(recvAddr.getSpec())
                    .withMessageRedelivery(new MessageRedelivery())
                    .withDeadLetterAddress(null)
                    .endSpec()
                    .build());
        }

        TestUtils.waitUntilCondition(() -> {
            try {
                assertRedeliveryStatus(recvAddr, null);
                return true;
            } catch (Exception | AssertionError e) {
                return false;
            }
        }, Duration.ofMinutes(2), Duration.ofSeconds(15));

        log.info("Successfully removed redelivery/DLQ settings");
        awaitAddressSettingsSync(addressSpace, recvAddr);

        sendAndReceiveFailingDeliveries(addressSpace, addr, recvAddr, user, List.of(createMessage(addr)));
    }

    private void sendAndReceiveFailingDeliveries(AddressSpace addressSpace, Address sendAddr, Address recvAddr, UserCredentials user, List<Message> messages) throws Exception {
        sendAddr = resourcesManager.getAddress(sendAddr.getMetadata().getNamespace(), sendAddr);
        MessageRedelivery redelivery = sendAddr.getStatus().getMessageRedelivery() == null ? new MessageRedelivery() : sendAddr.getStatus().getMessageRedelivery();

        AddressType addressType = AddressType.getEnum(sendAddr.getSpec().getType());
        try(AmqpClient client = addressType == AddressType.TOPIC ? getAmqpClientFactory().createTopicClient(addressSpace) : getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> sent = client.sendMessages(sendAddr.getSpec().getAddress(), messages, (message -> count.getAndIncrement() == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            AtomicInteger totalDeliveries = new AtomicInteger();
            String recvAddress = AddressType.getEnum(recvAddr.getSpec().getType()) == AddressType.SUBSCRIPTION ?  sendAddr.getSpec().getAddress() + "::" + recvAddr.getSpec().getAddress() : recvAddr.getSpec().getAddress();
            Source source = createSource(recvAddress);
            int expected = messages.size() * Math.max(redelivery.getMaximumDeliveryAttempts() == null ? 0 : redelivery.getMaximumDeliveryAttempts(), 1);
            assertThat("unexpected number of failed deliveries", client.recvMessages(source, message -> {
                log.info("message: {}, delivery count: {}", message.getMessageId(), message.getHeader().getDeliveryCount());
                return totalDeliveries.incrementAndGet() >= expected;}, Optional.empty(),
                    delivery -> delivery.disposition(DELIVERY_FAILED, true)).getResult().get(1, TimeUnit.MINUTES).size(), is(expected));

            if (redelivery.getMaximumDeliveryAttempts() != null && redelivery.getMaximumDeliveryAttempts() >= 0) {
                if (sendAddr.getSpec().getDeadLetterAddress() != null) {
                    // Messages should have been routed to the dead letter address
                    assertThat("all messages should have been routed to the dead letter address", client.recvMessages(sendAddr.getSpec().getDeadLetterAddress(), 1).get(1, TimeUnit.MINUTES).size(), is(messages.size()));
                }
            } else {
                // Infinite delivery attempts configured - consume normally
                assertThat("all messages should have been eligible for consumption", client.recvMessages(recvAddress, 1).get(1, TimeUnit.MINUTES).size(), is(messages.size()));
            }
        }
    }

    private void assertRedeliveryStatus(Address addr, MessageRedelivery expectedRedelivery) {
        Address reread = resourcesManager.getAddress(addr.getMetadata().getNamespace(), addr);
        if (expectedRedelivery == null) {
            assertThat(reread.getStatus().getMessageTtl(), nullValue());
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
    private void awaitAddressSettingsSync(AddressSpace addressSpace, Address addr) {
        Address reread = resourcesManager.getAddress(addr.getMetadata().getNamespace(), addr);
        MessageRedelivery expectedRedelivery = reread.getStatus().getMessageRedelivery() == null ? new MessageRedelivery() : reread.getStatus().getMessageRedelivery();

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
            Map<String, Object> actualSettings = ArtemisUtils.getAddressSettings(kubernetes, name, supportCredentials, reread.getSpec().getAddress());
            int maxDeliveryAttempts = ((Number) actualSettings.get("maxDeliveryAttempts")).intValue();
            String deadLetterAddress = String.valueOf(actualSettings.get("DLA"));
            String expiryAddress = String.valueOf(actualSettings.get("expiryAddress"));
            boolean b = maxDeliveryAttemptsEquals(maxDeliveryAttempts, expectedRedelivery.getMaximumDeliveryAttempts());
            if (!b) {
                log.info("Address {} on broker {} does not have expected redelivery values: {}, dead letter: {}, expiry: {}." +
                                " Actual maxDeliveryAttempts: {}, dead letter: {} expiry: {}",
                        reread.getMetadata().getName(), name,
                        expectedRedelivery, reread.getSpec().getDeadLetterAddress(), reread.getSpec().getExpiryAddress(),
                        maxDeliveryAttempts, deadLetterAddress, expiryAddress);
            }
            return b;
                }, Duration.ofMinutes(2), Duration.ofSeconds(5),
                () -> fail("Failed to await address settings to sync")));
    }

    private boolean maxDeliveryAttemptsEquals(int artemisMaxDeliveryAttempts, Integer maximumDeliveryAttempts) {
        return Objects.equals(maximumDeliveryAttempts == null ? -1 : maximumDeliveryAttempts, artemisMaxDeliveryAttempts);
    }

    private Source createSource(String recvAddress) {
        Source source = new Source();
        source.setAddress(recvAddress);
        return source;
    }

    private Message createMessage(Address addr) {
        Message msg = Message.Factory.create();
        msg.setMessageId(UUID.randomUUID());
        msg.setAddress(addr.getSpec().getAddress());
        msg.setDurable(true);
        return msg;
    }
}

