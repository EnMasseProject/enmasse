/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common;

import org.junit.jupiter.api.Test;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.bases.common.PersistentMessagesTestBase;
import io.enmasse.systemtest.utils.AddressUtils;

public class StandardPersistentMessagesTest extends PersistentMessagesTestBase {

    @Override
    public String getDefaultAddrSpaceIdentifier() {
        return "standard-persistent";
    }

    @Override
    public AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }

    @Override
    public String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.STANDARD_UNLIMITED;
    }

    @Test
    void testSmallQueue() throws Exception {
        Address standardQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-standard")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        doTestQueuePersistentMessages(standardQueue, 30);
    }

    @Test
    void testLargeQueue() throws Exception {
        Address standardLargeQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-large-queue-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-large-queue-standard")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        doTestQueuePersistentMessages(standardLargeQueue, 30);
    }

    @Test
    void testXLargeQueue() throws Exception {
        Address standardXLargeQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-xlarge-queue-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-xlarge-queue-standard")
                .withPlan(DestinationPlan.STANDARD_XLARGE_QUEUE)
                .endSpec()
                .build();
        doTestQueuePersistentMessages(standardXLargeQueue, 30);
    }

    @Test
    void testTopic() throws Exception {
        Address standardTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-topic-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic-standard")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        Address standardSub = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-sub-standard"))
                .endMetadata()
                .withNewSpec()
                .withType("subscription")
                .withAddress("test-sub-standard")
                .withTopic(standardTopic.getSpec().getAddress())
                .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                .endSpec()
                .build();
        doTestTopicPersistentMessages(standardTopic, standardSub);
    }

}
