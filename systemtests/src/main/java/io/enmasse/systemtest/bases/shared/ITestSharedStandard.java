/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.shared;

import io.enmasse.systemtest.bases.ITestBase;
import io.enmasse.systemtest.bases.ITestBaseStandard;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import org.junit.jupiter.api.Tag;

import static io.enmasse.systemtest.TestTag.SHARED_STANDARD;

@Tag(SHARED_STANDARD)
public interface ITestSharedStandard extends ITestBaseStandard, ITestBaseShared, ITestBase {

    @Override
    default String getDefaultAddressSpacePlan() {
        return AddressSpacePlans.STANDARD_UNLIMITED;
    }

    @Override
    default String getDefaultAddrSpaceIdentifier() {
        return "standard";
    }


}
