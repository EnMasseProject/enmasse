package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.BrokeredWebConsoleTest;
import io.enmasse.systemtest.Destination;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeWebConsoleTest extends BrokeredWebConsoleTest {

    //@Test
    public void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(Destination.queue("test-queue"));
    }

    //@Test
    public void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(Destination.topic("test-topic"));
    }

    @Override
    protected WebDriver buildDriver() {
        ChromeOptions opts = new ChromeOptions();
        opts.setHeadless(true);
        return new ChromeDriver(opts);
    }
}
