/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Test;

class MulticastTest extends TestBase implements ITestSharedStandard {

    @Test
    void testRestApi() throws Exception {
        Address m1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "multicast1"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("multicast1")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();
        Address m2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "multicast2"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("multicast2")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();

        TestUtils.runRestApiTest(resourcesManager, getSharedAddressSpace(), m1, m2);
    }
}
