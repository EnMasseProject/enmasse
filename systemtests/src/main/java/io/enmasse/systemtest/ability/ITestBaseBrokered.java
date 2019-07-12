/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.DestinationPlan;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.TestTag.sharedBrokered;


@Tag(sharedBrokered)
public interface ITestBaseBrokered extends ITestBase {

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }

    @Override
    default String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return DestinationPlan.BROKERED_QUEUE;
            case TOPIC:
                return DestinationPlan.BROKERED_TOPIC;
        }
        return null;
    }

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.BROKERED;
    }

    @Override
    default String getDefaultAddrSpaceIdentifier() {
        return "brokered";
    }
}
