/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.plans.web;

import io.enmasse.systemtest.bases.web.WebConsolePlansTest;
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

class FirefoxWebConsolePlansTest extends WebConsolePlansTest implements ISeleniumProviderFirefox {

    @Test
    void testCreateAddressPlan() throws Exception {
        doTestCreateAddressPlan();
    }
}
