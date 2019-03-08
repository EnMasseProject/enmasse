/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Test;

class MulticastTest extends TestBaseWithShared implements ITestBaseStandard {

    @Test
    void testRestApi() throws Exception {
        Address m1 = AddressUtils.createMulticastAddressObject("multicastRest1");
        Address m2 = AddressUtils.createMulticastAddressObject("multicastRest2");

        runRestApiTest(sharedAddressSpace, m1, m2);
    }
}
