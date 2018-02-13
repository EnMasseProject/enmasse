/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

public class FirefoxWebConsoleTest extends WebConsoleTest {

    @Test
    public void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong();
    }

    @Override
    public WebDriver buildDriver() {
        return getFirefoxDriver();
    }
}
