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
