package io.enmasse.systemtest.marathon;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class FirefoxWebConsoleTest extends WebConsoleTest {

    @Test
    public void testCreateDeleteAddressesViaAgentLong() throws Exception {
        doTestCreateDeleteAddressesViaAgentLong();
    }

    @Override
    public WebDriver buildDriver() {
        FirefoxOptions opts = new FirefoxOptions();
        opts.setHeadless(true);
        return new FirefoxDriver(opts);
    }
}
