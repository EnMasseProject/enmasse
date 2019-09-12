/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.common.PersistentMessagesTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Test;

public class BrokeredPersistentMessagesTest extends PersistentMessagesTestBase implements ITestSharedBrokered {

    @Override
    public String getDefaultAddrSpaceIdentifier() {
        return "brokered-persistent";
    }

    @Override
    public AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }

    @Override
    public String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.BROKERED;
    }

    @Test
    void testBrokeredQueue() throws Exception {
        Address brokeredQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue-brokered"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-brokered")
                .withPlan(DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();
        doTestQueuePersistentMessages(brokeredQueue, 100);
    }

}
