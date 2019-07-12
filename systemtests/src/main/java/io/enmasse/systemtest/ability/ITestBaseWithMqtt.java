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

import static io.enmasse.systemtest.TestTag.sharedMqtt;

@Tag(sharedMqtt)
public interface ITestBaseWithMqtt extends ITestBase {

    @Override
    default String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case TOPIC:
                return DestinationPlan.STANDARD_LARGE_TOPIC;
            case SUBSCRIPTION:
                return DestinationPlan.STANDARD_SMALL_SUBSCRIPTION;
            default:
                return null;
        }
    }

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT;
    }

    @Override
    default String getDefaultAddrSpaceIdentifier() {
        return "mqtt";
    }
}
