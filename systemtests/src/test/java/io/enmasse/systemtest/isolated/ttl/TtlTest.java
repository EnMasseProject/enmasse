/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.ttl;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag(ISOLATED)
class TtlTest extends TestBase implements ITestBaseIsolated {

    @ParameterizedTest(name = "tesAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void tesAddressSpecified(String type) throws Exception {
        doTestTtl(type, null, new TtlBuilder().withMinimum(500L).withMaximum(5000L).build(),
                new TtlBuilder().withMinimum(500L).withMaximum(5000L).build());
    }

    @ParameterizedTest(name = "tesAddressPlanSpecifiedTtl-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void tesAddressPlanSpecified(String type) throws Exception {
        doTestTtl(type, new TtlBuilder().withMinimum(500L).withMaximum(5000L).build(), null,
                new TtlBuilder().withMinimum(500L).withMaximum(5000L).build());
    }

    @ParameterizedTest(name = "testOverriding-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testOverriding(String type) throws Exception {
        doTestTtl(type, new TtlBuilder().withMinimum(500L).withMaximum(5000L).build(), new TtlBuilder().withMinimum(550L).withMaximum(6000L).build(),
                new TtlBuilder().withMinimum(550L /* higher addr min takes priority */).withMaximum(5000L /* lower max plan takes priority */).build());
    }

    private void doTestTtl(String type, Ttl addrPlanTtl, Ttl addrTtl, Ttl expectedTtl) throws Exception {
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

        if ("standard".equals(type)) {
            baseSpacePlan =  AddressSpacePlans.STANDARD_SMALL;
            baseAddressPlan = DestinationPlan.STANDARD_MEDIUM_QUEUE;
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
            baseAddressPlan = DestinationPlan.BROKERED_QUEUE;
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
                .withTtl(addrPlanTtl)
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

        Address addrWithTtl = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-ttl"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withTtl(addrTtl)
                .withAddress("message-ttl")
                .withPlan(addrPlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        isolatedResourcesManager.setAddresses(addrWithTtl);

        assertTtlStatus(addrWithTtl, expectedTtl);

//        TestUtils.waitUntilCondition("artemis to be in synch", waitPhase -> {
//            Map<String, Object> actualSettings = ArtemisUtils.getAddressSettings(kubernetes, addressSpace, addrWithTtl.getSpec().getAddress());
//            return Objects.equals(expectedTtl.getMinimum() == null ? -1 : expectedTtl.getMinimum(), ((Number) actualSettings.get("minExpiryDelay")).longValue()) &&
//                    Objects.equals(expectedTtl.getMaximum() == null ? -1  : expectedTtl.getMaximum(), ((Number) actualSettings.get("maxExpiryDelay")).longValue());
//        }, new TimeoutBudget(1, TimeUnit.MINUTES));

        UserCredentials user = new UserCredentials("user", "passwd");
        isolatedResourcesManager.createOrUpdateUser(addressSpace, user);


        try(AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            List<Message> messages = new ArrayList<>();
            AtomicInteger count = new AtomicInteger();
            List.of(0L,
                    (expectedTtl.getMaximum() - expectedTtl.getMinimum() / 2) + expectedTtl.getMinimum(),
                    Duration.ofDays(1).toMillis()).forEach(expiry -> {
                Message msg = Message.Factory.create();
                msg.setAddress(addrWithTtl.getSpec().getAddress());
                msg.setDurable(true);
                if (expiry > 0) {
                    msg.setExpiryTime(expiry);
                }
                messages.add(msg);
            });

            CompletableFuture<Integer> sent = client.sendMessages(addrWithTtl.getSpec().getAddress(), messages, (message -> count.getAndIncrement()  == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            Thread.sleep(expectedTtl.getMaximum() + (messageExpiryScanPeriod * 2));

            assertThrows(TimeoutException.class, () -> {
                List<Message> received = client.recvMessages(addrWithTtl.getSpec().getAddress(), (message) -> true).get(20, TimeUnit.SECONDS);
                assertThat("all messages should have expired", received.size(), is(0));
            });
        }

        // Now remove the TTL restriction from the plan/address
        // and send
        if (addrPlan.getSpec().getTtl() != null) {
            isolatedResourcesManager.replaceAddressPlan(new AddressPlanBuilder()
                    .withMetadata(addrPlan.getMetadata())
                    .withNewSpecLike(addrPlan.getSpec())
                    .withTtl(new Ttl())
                    .endSpec()
                    .build());
        }

        if (addrWithTtl.getSpec().getTtl() != null) {
            isolatedResourcesManager.replaceAddress(new AddressBuilder()
                    .withMetadata(addrWithTtl.getMetadata())
                    .withNewSpecLike(addrWithTtl.getSpec())
                    .withTtl(new Ttl())
                    .endSpec()
                    .build());
        }

        TestUtils.waitUntilCondition(() -> {
            try {
                assertTtlStatus(addrWithTtl, null);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, Duration.ofMinutes(2), Duration.ofSeconds(15));

        try(AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(user);

            List<Message> messages = new ArrayList<>();
            AtomicInteger count = new AtomicInteger();
            count.set(0);
            IntStream.of(1).forEach(unused -> {
                Message msg = Message.Factory.create();
                msg.setAddress(addrWithTtl.getSpec().getAddress());
                msg.setDurable(true);
                messages.add(msg);
            });

            CompletableFuture<Integer> sent = client.sendMessages(addrWithTtl.getSpec().getAddress(), messages, (message -> count.getAndIncrement()  == messages.size()));
            assertThat("all messages should have been sent", sent.get(20, TimeUnit.SECONDS), is(messages.size()));

            Thread.sleep(messageExpiryScanPeriod * 2);  // Give sufficient time for erroneous expiration

            Future<List<Message>> received = client.recvMessages(addrWithTtl.getSpec().getAddress(),1);
            assertThat("message should not have expired", received.get(20, TimeUnit.SECONDS).size(), is(1));
        }
    }

    private void assertTtlStatus(Address addrWithTtl, Ttl expectedTtl) {
        Address reread = resourcesManager.getAddress(addrWithTtl.getMetadata().getNamespace(), addrWithTtl);
        if (expectedTtl == null) {
            assertThat(reread.getStatus().getTtl(), nullValue());
        } else {
            assertThat(reread.getStatus().getTtl(), notNullValue());
            if (expectedTtl.getMinimum() != null) {
                assertThat(reread.getStatus().getTtl().getMinimum(), is(expectedTtl.getMinimum()));
            } else {
                assertThat(reread.getStatus().getTtl().getMinimum(), nullValue());
            }
            if (expectedTtl.getMaximum() != null) {
                assertThat(reread.getStatus().getTtl().getMaximum(), is(expectedTtl.getMaximum()));
            } else {
                assertThat(reread.getStatus().getTtl().getMaximum(), nullValue());
            }

        }
    }


}

