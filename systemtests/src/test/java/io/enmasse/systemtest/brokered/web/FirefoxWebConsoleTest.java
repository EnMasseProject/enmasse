package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.BrokeredWebConsoleTest;
import io.enmasse.systemtest.Destination;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class FirefoxWebConsoleTest extends BrokeredWebConsoleTest {

    @Test
    public void testCreateQueue() throws Exception {
        doTestCreateAddress(Destination.queue("test-queue"));
    }

    @Test
    public void testCreateTopic() throws Exception {
        doTestCreateAddress(Destination.topic("test-topic"));
    }


    @Override
    protected WebDriver buildDriver() {
        FirefoxOptions opts = new FirefoxOptions();
        opts.setHeadless(true);
        return new FirefoxDriver(opts);
    }
}
