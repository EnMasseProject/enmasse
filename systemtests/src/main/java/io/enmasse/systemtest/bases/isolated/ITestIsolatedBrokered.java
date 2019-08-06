/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.isolated;

import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.bases.ITestBaseBrokered;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.TestTag.ISOLATED_BROKER;


@Tag(ISOLATED_BROKER)
public interface ITestIsolatedBrokered extends ITestBaseBrokered, ITestBaseIsolated, ITestBase {

    @Override
    default AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.BROKERED;
    }
}
