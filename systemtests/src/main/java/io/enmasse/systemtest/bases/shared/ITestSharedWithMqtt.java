/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.shared;

import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.TestTag.SHARED_MQTT;

@Tag(SHARED_MQTT)
public interface ITestSharedWithMqtt extends ITestBase, ITestBaseShared {

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
