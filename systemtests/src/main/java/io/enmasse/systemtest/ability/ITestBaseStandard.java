/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.DestinationPlan;

public interface ITestBaseStandard extends ITestBase {

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }

    @Override
    default String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return DestinationPlan.STANDARD_SMALL_QUEUE.plan();
            case TOPIC:
                return DestinationPlan.STANDARD_SMALL_TOPIC.plan();
            case ANYCAST:
                return DestinationPlan.STANDARD_SMALL_ANYCAST.plan();
            case MULTICAST:
                return DestinationPlan.STANDARD_SMALL_MULTICAST.plan();
        }
        return null;
    }

    @Override
    default boolean skipDummyAddress() {
        return false;
    }
}
