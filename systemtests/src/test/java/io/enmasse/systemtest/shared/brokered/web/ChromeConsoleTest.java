/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumChrome;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.SHARED;

/**
*
* We are only adding a few tests for chrome because, the rest of the functionality is covered by firefox.
* Among other reasons, for not making the test runs too long.
*
*/
@Tag(NON_PR)
@Tag(SHARED)
@SeleniumChrome
class ChromeConsoleTest extends ConsoleTest {

    @BeforeAll
    void initMessaging() throws Exception {
        resourceManager.createDefaultMessaging(AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED);
    }

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(resourceManager.getDefaultAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }

    @Test
    void testFilterAddressesByType() throws Exception {
        doTestFilterAddressesByType(resourceManager.getDefaultAddressSpace());
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName(resourceManager.getDefaultAddressSpace());
    }

    @Test
    @ExternalClients
    void testAddressLinks() throws Exception {
        doTestAddressLinks(resourceManager.getDefaultAddressSpace(), DestinationPlan.BROKERED_QUEUE);
    }

}
