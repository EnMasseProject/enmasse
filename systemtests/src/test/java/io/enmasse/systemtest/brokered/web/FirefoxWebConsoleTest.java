package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.WebBrowserType;
import org.junit.Test;

public class FirefoxWebConsoleTest extends WebConsoleTest {

    @Test
    public void testCreateQueue() throws Exception {
        doTestCreateAddress(Destination.queue("test-queue"));
    }

    @Test
    public void testCreateTopic() throws Exception {
        doTestCreateAddress(Destination.topic("test-topic"));
    }

    @Override
    protected WebBrowserType getWebBrowserType() {
        return WebBrowserType.FIREFOX;
    }
}
