package io.enmasse.systemtest.marathon;

import org.openqa.selenium.WebDriver;

public class ChromeWebConsoleTest extends WebConsoleTest {

    //@Test
    public void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong();
    }

    @Override
    public WebDriver buildDriver() {
        return getChromeDriver();
    }
}
