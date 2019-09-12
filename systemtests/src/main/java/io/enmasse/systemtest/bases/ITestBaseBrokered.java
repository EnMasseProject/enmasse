/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;

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
}
