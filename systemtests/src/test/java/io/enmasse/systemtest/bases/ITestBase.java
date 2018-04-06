/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;

public interface ITestBase {
    default AddressSpaceType getAddressSpaceType() {
        return null;
    }

    default String getDefaultPlan(AddressType addressType) {
        return null;
    }

    default boolean skipDummyAddress() {
        return true;
    }
}
