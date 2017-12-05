package io.enmasse.systemtest;


public abstract class WebConsoleTest extends SeleniumTestBase {

    public void doTestCreateAddress(Destination destination) throws Exception {
        createAddressWebConsole(destination);
    }
}
