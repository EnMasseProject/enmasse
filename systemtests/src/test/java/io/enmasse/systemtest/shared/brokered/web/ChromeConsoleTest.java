/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.bases.web.ConsoleTest;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.selenium.SeleniumChrome;
import io.enmasse.systemtest.utils.AddressUtils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static io.enmasse.systemtest.TestTag.NON_PR;

/**
*
* We are only adding a few tests for chrome because, the rest of the functionality is covered by firefox.
* Among other reasons, for not making the test runs too long.
*
*/
@Disabled
@Tag(NON_PR)
@SeleniumChrome
class ChromeConsoleTest extends ConsoleTest implements ITestSharedBrokered {

    @Test
    void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(getSharedAddressSpace(), new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }

    @Test
    void testFilterAddressesByType() throws Exception {
        doTestFilterAddressesByType(getSharedAddressSpace());
    }

    @Test
    void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName(getSharedAddressSpace());
    }

}
