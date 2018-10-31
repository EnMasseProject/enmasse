/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.ability;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.DestinationPlan;

public interface ITestBaseWithoutMqtt extends ITestBase {

    @Override
    default String getDefaultPlan(AddressType addressType) {
        return DestinationPlan.STANDARD_SMALL_ANYCAST.plan();
    }

    default String getAddressSpacePlan() {
        return "standard-small";
    }
}
