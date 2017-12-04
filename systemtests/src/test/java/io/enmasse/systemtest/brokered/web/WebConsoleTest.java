package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.BrokeredSeleniumTestBase;
import io.enmasse.systemtest.Destination;

public abstract class WebConsoleTest extends BrokeredSeleniumTestBase {

    public void doTestCreateAddress(Destination destination) throws Exception {
        createAddressWebConsole(destination);
    }
}
