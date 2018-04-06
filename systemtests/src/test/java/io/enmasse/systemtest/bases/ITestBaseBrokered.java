/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;

public interface ITestBaseBrokered extends ITestBase {

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }

    @Override
    default String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return "brokered-queue";
            case TOPIC:
                return "brokered-topic";
        }
        return null;
    }

    @Override
    default boolean skipDummyAddress() {
        return true;
    }
}
