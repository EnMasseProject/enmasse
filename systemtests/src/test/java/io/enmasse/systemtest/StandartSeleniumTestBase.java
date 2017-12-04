package io.enmasse.systemtest;

public class StandartSeleniumTestBase extends SeleniumTestBase {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }
}
