/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.plans.web;

import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.web.WebConsolePlansTest;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import org.junit.jupiter.api.Test;

@SeleniumFirefox
class FirefoxWebConsolePlansTest extends WebConsolePlansTest implements ITestIsolatedStandard {

    @Test
    void testCreateAddressPlan() throws Exception {
        doTestCreateAddressPlan();
    }
}
