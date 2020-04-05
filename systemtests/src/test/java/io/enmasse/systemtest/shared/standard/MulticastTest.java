/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;

@Tag(SHARED)
class MulticastTest extends TestBase {

    @BeforeAll
    void initMessaging() throws Exception {
        resourceManager.createDefaultMessaging(AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_UNLIMITED);
    }

    @Test
    void testRestApi() throws Exception {
        Address m1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "multicast1"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("multicast1")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();
        Address m2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "multicast2"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("multicast2")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();

        assertAddressApi(resourceManager.getDefaultAddressSpace(), m1, m2);
    }
}
