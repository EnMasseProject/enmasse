/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.ttl;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.address.model.MessageTtl;
import io.enmasse.address.model.MessageTtlBuilder;
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
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(ISOLATED)
@Tag(ACCEPTANCE)
class MessageTtlTest extends TestBase implements ITestBaseIsolated {
    private static Logger log = CustomLogger.getLogger();

    @ParameterizedTest(name = "testAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressSpecified(String type) throws Exception {
        doTestTtl(AddressSpaceType.getEnum(type), AddressType.QUEUE, null, new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build(),
                new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build());
    }

    @ParameterizedTest(name = "testAddressPlanSpecifiedTtl-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressPlanSpecified(String type) throws Exception {
        doTestTtl(AddressSpaceType.getEnum(type), AddressType.QUEUE, new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build(), null,
                new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build());
    }

    @ParameterizedTest(name = "testOverriding-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testOverriding(String type) throws Exception {
        doTestTtl(AddressSpaceType.getEnum(type), AddressType.QUEUE, new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build(), new MessageTtlBuilder().withMinimum(550L).withMaximum(6000L).build(),
                new MessageTtlBuilder().withMinimum(550L /* higher addr min takes priority */).withMaximum(5000L /* lower max plan takes priority */).build());
    }

    @ParameterizedTest(name = "testAddressPlanSpecifiedMaxTtl-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testAddressPlanSpecifiedMaxTtl(String type) throws Exception {
        doTestTtl(AddressSpaceType.getEnum(type), AddressType.QUEUE, new MessageTtlBuilder().withMaximum(5000L).build(), null,
                new MessageTtlBuilder().withMaximum(5000L).build());
    }

    @ParameterizedTest(name = "testTopicAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard"})
    void testTopicAddressSpecified(String type) throws Exception {
        doTestTtl(AddressSpaceType.getEnum(type), AddressType.TOPIC, null, new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build(),
                new MessageTtlBuilder().withMinimum(500L).withMaximum(5000L).build());
    }

    private void doTestTtl(AddressSpaceType addressSpaceType, AddressType addressType, MessageTtl addrPlanTtl, MessageTtl addrTtl, MessageTtl expectedTtl) throws Exception {
        final String infraConfigName = "ttl-infra";
        final String spacePlanName = "space-plan-ttl";
        final String addrPlanName = "addr-plan-ttl";

        final String baseSpacePlan;
        final String baseAddressPlan;
        final long messageExpiryScanPeriod = 1000L;

        PodTemplateSpec brokerInfraTtlOverride = new PodTemplateSpecBuilder()
                .withNewSpec()
                .withInitContainers(new ContainerBuilder()
                        .withName("broker-plugin")
                        .withEnv(new EnvVar("MESSAGE_EXPIRY_SCAN_PERIOD", String.format("%d", messageExpiryScanPeriod), null)).build()).endSpec().build();

        if (AddressSpaceType.STANDARD == addressSpaceType) {
            baseSpacePlan =  AddressSpacePlans.STANDARD_SMALL;
            baseAddressPlan = addressType == AddressType.QUEUE ? DestinationPlan.STANDARD_MEDIUM_QUEUE : DestinationPlan.STANDARD_SMALL_TOPIC;
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

        AddressPlan addrPlan = new AddressPlanBuilder()
                .withNewMetadata()
                .withName(addrPlanName)
                .endMetadata()
                .withNewSpecLike(smallPlan.getSpec())
                .withMessageTtl(addrPlanTtl)
                .endSpec()
                .build();

        AddressSpacePlan spacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(spacePlanName)
                .endMetadata()
                .withNewSpecLike(smallSpacePlan.getSpec())
                .withAddressPlans(addrPlan.getMetadata().getName())
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

        Address addr = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-ttl"))
                .endMetadata()
                .withNewSpec()
                .withType(addressType.toString())
                .withMessageTtl(addrTtl)
                .withAddress("message-ttl")
                .withPlan(addrPlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        isolatedResourcesManager.setAddresses(addr);

        Address recvAddr;
        if (addressType == AddressType.TOPIC && AddressSpaceType.STANDARD == addressSpaceType) {
                recvAddr = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(kubernetes.getInfraNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-ttl-sub"))
                        .endMetadata()
                        .withNewSpec()
                        .withType(AddressType.SUBSCRIPTION.toString())
                        .withMessageTtl(addrTtl)
                        .withAddress("message-ttl-sub")
                        .withTopic(addr.getSpec().getAddress())
                        .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                        .endSpec()
                        .build();
                isolatedResourcesManager.setAddresses(recvAddr);
        } else {
            recvAddr = addr;
        }

        assertTtlStatus(addr, expectedTtl);
        awaitAddressSettingsSync(addressSpace, addr, expectedTtl);

        UserCredentials user = new UserCredentials("user", "passwd");
        isolatedResourcesManager.createOrUpdateUser(addressSpace, user);

        final List<Message> messages = new ArrayList<>();
        long minimum = expectedTtl.getMinimum() == null ? 0 : expectedTtl.getMinimum();
        long maximum = expectedTtl.getMaximum();
        List.of(0L,
                (maximum - minimum / 2) + minimum,
                Duration.ofDays(1).toMillis()).forEach(expiry -> {
            Message msg = Message.Factory.create();
            msg.setAddress(addr.getSpec().getAddress());
            msg.setDurable(true);
            if (expiry > 0) {
                msg.setExpiryTime(expiry);
            }
            messages.add(msg);
        });

        doSendReceive(addressSpace, addr, recvAddr, user, messages, expectedTtl.getMaximum() + messageExpiryScanPeriod * 2, false);

        // Now remove the TTL restriction from the plan/address
        // and send again to confirm that messages are no longer subjected to TTL
        if (addrPlan.getSpec().getMessageTtl() != null) {
            isolatedResourcesManager.replaceAddressPlan(new AddressPlanBuilder()
                    .withMetadata(addrPlan.getMetadata())
                    .withNewSpecLike(addrPlan.getSpec())
                    .withMessageTtl(new MessageTtl())
                    .endSpec()
                    .build());
        }

        if (addr.getSpec().getMessageTtl() != null) {
            isolatedResourcesManager.replaceAddress(new AddressBuilder()
                    .withMetadata(addr.getMetadata())
                    .withNewSpecLike(addr.getSpec())
                    .withMessageTtl(new MessageTtl())
                    .endSpec()
                    .build());
        }

        TestUtils.waitUntilCondition(() -> {
            try {
                assertTtlStatus(addr, null);
                return true;
            } catch (Exception | AssertionError e) {
                return false;
            }
        }, Duration.ofMinutes(2), Duration.ofSeconds(15));

        awaitAddressSettingsSync(addressSpace, addr, new MessageTtl());

        Message msg = Message.Factory.create();
        msg.setAddress(addr.getSpec().getAddress());
        msg.setDurable(true);

        doSendReceive(addressSpace, addr, recvAddr, user, List.of(msg), messageExpiryScanPeriod * 2, true);
    }

    private void doSendReceive(AddressSpace addressSpace, Address sendAddr, Address recvAddr, UserCredentials user, List<Message> messages, long waitTime, boolean expectReceive) throws Exception {
        AddressType addressType = AddressType.getEnum(sendAddr.getSpec().getType());
        try(AmqpClient client = addressType == AddressType.TOPIC ? getAmqpClientFactory().createTopicClient(addressSpace) : getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            // TODO: for a brokered topic test, we need to be able to create the receiver first but not grant credit until after the
            // messages are sent.  The test framework does not currently permit this.

            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> sent = client.sendMessages(sendAddr.getSpec().getAddress(), messages, (message -> count.getAndIncrement()  == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            Thread.sleep(waitTime);  // Give sufficient time for an expiration

            String recvAddress = AddressType.getEnum(recvAddr.getSpec().getType()) == AddressType.SUBSCRIPTION ?  sendAddr.getSpec().getAddress() + "::" + recvAddr.getSpec().getAddress() : recvAddr.getSpec().getAddress();
            if (expectReceive) {
                Future<List<Message>> received = client.recvMessages(recvAddress, messages.size());
                assertThat("message should not have expired", received.get(20, TimeUnit.SECONDS).size(), is(messages.size()));
            } else {
                assertThrows(TimeoutException.class, () -> {
                    List<Message> received = client.recvMessages(recvAddress, (message) -> true).get(20, TimeUnit.SECONDS);
                    assertThat("all messages should have expired", received.size(), is(0));
                });
            }
        }
    }

    private void assertTtlStatus(Address addrWithTtl, MessageTtl expectedTtl) {
        Address reread = resourcesManager.getAddress(addrWithTtl.getMetadata().getNamespace(), addrWithTtl);
        if (expectedTtl == null) {
            assertThat(reread.getStatus().getMessageTtl(), nullValue());
        } else {
            assertThat(reread.getStatus().getMessageTtl(), notNullValue());
            if (expectedTtl.getMinimum() != null) {
                assertThat(reread.getStatus().getMessageTtl().getMinimum(), is(expectedTtl.getMinimum()));
            } else {
                assertThat(reread.getStatus().getMessageTtl().getMinimum(), nullValue());
            }
            if (expectedTtl.getMaximum() != null) {
                assertThat(reread.getStatus().getMessageTtl().getMaximum(), is(expectedTtl.getMaximum()));
            } else {
                assertThat(reread.getStatus().getMessageTtl().getMaximum(), nullValue());
            }

        }
    }

    // It'd be better if the address's status reflected when the expiry/address settings spaces were applied
    // but with out current architecture, agent (for the standard case) doesn't write the address status.
    // For now peep at the broker(s)
    private void awaitAddressSettingsSync(AddressSpace addressSpace, Address addr, MessageTtl expectedTtl) {

        UserCredentials supportCredentials = ArtemisUtils.getSupportCredentials(addressSpace);
        List<String> brokers = new ArrayList<>();
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
            Address reread = resourcesManager.getAddress(addr.getMetadata().getNamespace(), addr);
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
            Map<String, Object> actualSettings = ArtemisUtils.getAddressSettings(kubernetes, name, supportCredentials, addr.getSpec().getAddress());
            long minExpiryDelay = ((Number) actualSettings.get("minExpiryDelay")).longValue();
            long maxExpiryDelay = ((Number) actualSettings.get("maxExpiryDelay")).longValue();
            boolean b = expiryEquals(minExpiryDelay, expectedTtl.getMinimum()) &&
                    expiryEquals(maxExpiryDelay, expectedTtl.getMaximum());
            if (!b) {
                log.info("Address {} on broker {} does not have expected TTL values {}, actual expiry min: {} max: {}",
                        addr.getMetadata().getName(), name, expectedTtl, maxExpiryDelay, maxExpiryDelay);
            }
            return b;
        }, Duration.ofMinutes(2), Duration.ofSeconds(5)));
    }

    private boolean expiryEquals(long artemisExpiry, Long ttlExpiry) {
        return Objects.equals(ttlExpiry == null ? -1 : ttlExpiry, artemisExpiry);
    }


}

