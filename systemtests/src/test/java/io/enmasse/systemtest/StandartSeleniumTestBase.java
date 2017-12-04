package io.enmasse.systemtest;

public abstract class StandartSeleniumTestBase extends SeleniumTestBase {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }
}
