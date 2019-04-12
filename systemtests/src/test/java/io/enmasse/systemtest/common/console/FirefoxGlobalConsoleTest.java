/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.console;

import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.bases.web.GlobalConsoleTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
class FirefoxGlobalConsoleTest extends GlobalConsoleTest implements ISeleniumProviderFirefox {
    private static Logger log = CustomLogger.getLogger();

    @Test
    void testLoginLogout() throws Exception {
        doTestOpen();
    }

    @Test
    void testCreateDeleteAddressSpace() throws Exception {
        doTestCreateAddressSpace(AddressSpaceUtils.createAddressSpaceObject("test-address-space-brokered",
                kubernetes.getNamespace(), AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED));
        doTestCreateAddressSpace(AddressSpaceUtils.createAddressSpaceObject("test-address-space-standard",
                kubernetes.getNamespace(), AddressSpaceType.STANDARD, AddressSpacePlans.STANDARD_SMALL));
    }

    @Test
    void testConnectToAddressSpaceConsole() throws Exception {
        doTestConnectToAddressSpaceConsole(AddressSpaceUtils.createAddressSpaceObject("test-address-space-console",
                kubernetes.getNamespace(), AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED));
    }
}

