/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.shared;

import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.bases.ITestBaseBrokered;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.TestTag.SHARED_BROKERED;

@Tag(SHARED_BROKERED)
public interface ITestSharedBrokered extends ITestBase, ITestBaseShared, ITestBaseBrokered {

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.BROKERED;
    }

    @Override
    default String getDefaultAddrSpaceIdentifier() {
        return "brokered";
    }
}
