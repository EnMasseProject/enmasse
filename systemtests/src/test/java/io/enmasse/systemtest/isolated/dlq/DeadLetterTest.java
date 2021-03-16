/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.dlq;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
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
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag(ISOLATED)
@Tag(ACCEPTANCE)
class DeadLetterTest extends TestBase implements ITestBaseIsolated {
    private static final Logger log = CustomLogger.getLogger();
    private static final Modified DELIVERY_FAILED = new Modified();
    private static final Rejected REJECTED = new Rejected();
    private static final String GLOBAL_DLQ = "!!GLOBAL_DLQ";

    {
        DELIVERY_FAILED.setDeliveryFailed(true);
    }

    @ParameterizedTest(name = "testAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressSpecified(String type) throws Exception {
        doTestMessageRedelivery(AddressSpaceType.getEnum(type), AddressType.QUEUE, null, new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build(),
                new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build());

        System.out.println("hello");
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
        PodTemplateSpec brokerInfraTtlOverride = getPodTemplateSpec();

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
                .withName("message-dl-space")
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
                .withMessageRedelivery(addrRedelivery)
                .withAddress("message-redelivery")
                .withDeadletter(deadletter.getSpec().getAddress())
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
                        .withAddress("message-redelivery-sub")
                        .withTopic(addr.getSpec().getAddress())
                        .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                        .endSpec()
                        .build();
                isolatedResourcesManager.setAddresses(recvAddr);
        } else {
            recvAddr = addr;
        }

        AddressUtils.assertRedeliveryStatus(addressType == AddressType.TOPIC ? addr : recvAddr, expectedRedelivery);
        AddressUtils.awaitAddressSettingsSync(addressSpace, recvAddr, null);

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

        if (recvAddr.getSpec().getMessageRedelivery() != null || recvAddr.getSpec().getDeadletter() != null) {
            isolatedResourcesManager.replaceAddress(new AddressBuilder()
                    .withMetadata(recvAddr.getMetadata())
                    .withNewSpecLike(recvAddr.getSpec())
                    .withMessageRedelivery(new MessageRedelivery())
                    .withDeadletter(null)
                    .endSpec()
                    .build());
        }

        TestUtils.waitUntilCondition(() -> {
            try {
                AddressUtils.assertRedeliveryStatus(recvAddr, null);
                return true;
            } catch (Exception | AssertionError e) {
                return false;
            }
        }, Duration.ofMinutes(2), Duration.ofSeconds(15));

        log.info("Successfully removed redelivery/DLQ settings");
        AddressUtils.awaitAddressSettingsSync(addressSpace, recvAddr, null);

        sendAndReceiveFailingDeliveries(addressSpace, addr, recvAddr, user, List.of(createMessage(addr)));
    }
    @Test
    void testModDeliveryFailedDeliveriesFlowToGlobalDeadLetter() throws Exception {
        doGlobalDlqTest(new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build(), new MessageRedeliveryBuilder().withMaximumDeliveryAttempts(2).build(), DELIVERY_FAILED, true);
    }

    @Test
    void testRejectedDeliveriesFlowToGlobalDeadLetter() throws Exception {
        doGlobalDlqTest(null, null, REJECTED, false);
    }

    private void doGlobalDlqTest(MessageRedelivery messageRedelivery, MessageRedelivery expectedRedelivery, DeliveryState state, boolean treatRejectAsUnmodifiedDeliveryFailed) throws Exception {
        final String infraConfigName = "redelivery-infra";
        final String spacePlanName = "space-plan-redelivery";
        final String addrPlanName = "addr-plan-redelivery";

        PodTemplateSpec brokerInfraTtlOverride = getPodTemplateSpec();

        final String baseSpacePlan = AddressSpacePlans.STANDARD_SMALL;
        final String baseAddressPlan = DestinationPlan.STANDARD_MEDIUM_QUEUE;
        StandardInfraConfig infraConfig = isolatedResourcesManager.getStandardInfraConfig("default");
        StandardInfraConfig ttlInfra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName(infraConfigName)
                .endMetadata()
                .withNewSpecLike(infraConfig.getSpec())
                .withNewBrokerLike(infraConfig.getSpec().getBroker())
                .withTreatRejectAsUnmodifiedDeliveryFailed(treatRejectAsUnmodifiedDeliveryFailed)
                .withPodTemplate(brokerInfraTtlOverride)
                .endBroker()
                .withGlobalDLQ(true)
                .endSpec()
                .build();
        ttlInfra.getSpec().getBroker().setTreatRejectAsUnmodifiedDeliveryFailed(false);
        isolatedResourcesManager.createInfraConfig(ttlInfra);

        AddressSpacePlan smallSpacePlan = kubernetes.getAddressSpacePlanClient().withName(baseSpacePlan).get();
        AddressPlan smallPlan = kubernetes.getAddressPlanClient().withName(baseAddressPlan).get();

        AddressPlan addrPlan = new AddressPlanBuilder()
                .withNewMetadata()
                .withName(addrPlanName)
                .endMetadata()
                .withNewSpecLike(smallPlan.getSpec())
                .withMessageRedelivery(null)
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
                .withName("message-dl-space")
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

        Address addr = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-redelivery"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withMessageRedelivery(messageRedelivery)
                .withAddress("message-redelivery")
                .withPlan(addrPlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        isolatedResourcesManager.setAddresses(addr);

        AddressUtils.assertRedeliveryStatus(addr, expectedRedelivery);
        AddressUtils.awaitAddressSettingsSync(addressSpace, addr, expectedRedelivery);

        UserCredentials user = new UserCredentials("user", "passwd");
        isolatedResourcesManager.createOrUpdateUser(addressSpace, user);

        List<Message> messages = List.of(createMessage(addr));
        Address sendAddr = resourcesManager.getAddress(addr.getMetadata().getNamespace(), addr);
        MessageRedelivery redelivery = sendAddr.getStatus().getMessageRedelivery() == null ? new MessageRedelivery() : sendAddr.getStatus().getMessageRedelivery();

        try(AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> sent = client.sendMessages(sendAddr.getSpec().getAddress(), messages, (message -> count.getAndIncrement() == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            AtomicInteger totalDeliveries = new AtomicInteger();
            Source source = createSource(addr.getSpec().getAddress());
            int expected = messages.size() * Math.max(redelivery.getMaximumDeliveryAttempts() == null || state instanceof Rejected ? 0 : redelivery.getMaximumDeliveryAttempts(), 1);
            assertThat("unexpected number of failed deliveries", client.recvMessages(source, message -> {
                        log.info("message: {}, delivery count: {}", message.getMessageId(), message.getHeader().getDeliveryCount());
                        return totalDeliveries.incrementAndGet() >= expected;}, Optional.empty(),
                    delivery -> delivery.disposition(state, true)).getResult().get(1, TimeUnit.MINUTES).size(), is(expected));

            if (state instanceof Rejected || (redelivery.getMaximumDeliveryAttempts() != null && redelivery.getMaximumDeliveryAttempts() >= 0)) {
                assertThat("all messages should have been routed to the dead letter address", client.recvMessages(GLOBAL_DLQ, 1).get(1, TimeUnit.MINUTES).size(), is(messages.size()));
            } else {
                // Infinite delivery attempts configured - consume normally
                assertThat("all messages should have been eligible for consumption", client.recvMessages(addr.getSpec().getAddress(), 1).get(1, TimeUnit.MINUTES).size(), is(messages.size()));
            }
        }
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
                if (recvAddr.getSpec().getDeadletter() != null) {
                    // Messages should have been routed to the dead letter address
                    assertThat("all messages should have been routed to the dead letter address", client.recvMessages(recvAddr.getSpec().getDeadletter(), 1).get(1, TimeUnit.MINUTES).size(), is(messages.size()));
                }
            } else {
                // Infinite delivery attempts configured - consume normally
                assertThat("all messages should have been eligible for consumption", client.recvMessages(recvAddress, 1).get(1, TimeUnit.MINUTES).size(), is(messages.size()));
            }
        }
    }

    @ParameterizedTest(name = "testDanglingDeadLetterReference-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testDanglingDeadLetterReference(String type) throws Exception {
        AddressSpaceType addressSpaceType = AddressSpaceType.getEnum(type);
        AddressType addressType = AddressType.QUEUE;
        final String baseSpacePlan;
        final String baseAddressPlan;
        final String deadLetterAddressPlan;

        if (AddressSpaceType.STANDARD == addressSpaceType) {
            baseSpacePlan =  AddressSpacePlans.STANDARD_SMALL;
            baseAddressPlan = DestinationPlan.STANDARD_MEDIUM_QUEUE;
            deadLetterAddressPlan = DestinationPlan.STANDARD_DEADLETTER;
        } else {
            baseSpacePlan = AddressSpacePlans.BROKERED;
            baseAddressPlan = DestinationPlan.BROKERED_QUEUE;
            deadLetterAddressPlan = DestinationPlan.BROKERED_DEADLETTER;
        }

        AddressSpacePlan spacePlan = kubernetes.getAddressSpacePlanClient().withName(baseSpacePlan).get();
        AddressPlan addrPlan = kubernetes.getAddressPlanClient().withName(baseAddressPlan).get();
        AddressPlan deadletterPlan = kubernetes.getAddressPlanClient().withName(deadLetterAddressPlan).get();

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("message-dl-space")
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
                .withAddress("message-redelivery")
                .withDeadletter(deadletter.getSpec().getAddress())
                .withPlan(addrPlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        isolatedResourcesManager.setAddresses(deadletter, addr);

        UserCredentials user = new UserCredentials("user", "passwd");
        isolatedResourcesManager.createOrUpdateUser(addressSpace, user);

        List<Message> messages = List.of(createMessage(addr), createMessage(addr));

        try(AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> sent = client.sendMessages(addr.getSpec().getAddress(), messages, (message -> count.getAndIncrement() == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            List<Message> received = client.recvMessages(addr.getSpec().getAddress(), 1).get(20, TimeUnit.SECONDS);
            assertThat("unexpected number of messages received before", received.size(), is(1));

            isolatedResourcesManager.deleteAddresses(deadletter);

            TestUtils.waitUntilCondition(() -> {
                try {
                    Address reread = resourcesManager.getAddress(addr.getMetadata().getNamespace(), addr);
                    Optional<String> found = reread.getStatus().getMessages().stream().filter(m -> m.contains("references a dead letter address 'deadletter' that does not exist")).findFirst();
                    return found.isPresent();
                } catch (Exception | AssertionError e) {
                    return false;
                }
            }, Duration.ofMinutes(2), Duration.ofSeconds(15));

            log.info("Dead letter ref now dangling, ensuring existing messages exist for consumption");

            received = client.recvMessages(addr.getSpec().getAddress(), 1).get(20, TimeUnit.SECONDS);
            assertThat("unexpected number of messages received after", received.size(), is(1));
        }
    }

    private PodTemplateSpec getPodTemplateSpec() {
        final long messageExpiryScanPeriod = 1000L;

        return new PodTemplateSpecBuilder()
                .withNewSpec()
                .withInitContainers(new ContainerBuilder()
                        .withName("broker-plugin")
                        .withEnv(new EnvVar("MESSAGE_EXPIRY_SCAN_PERIOD", String.format("%d", messageExpiryScanPeriod), null)).build()).endSpec().build();
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

