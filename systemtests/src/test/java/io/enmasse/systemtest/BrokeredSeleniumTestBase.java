package io.enmasse.systemtest;

public abstract class BrokeredSeleniumTestBase extends SeleniumTestBase {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }
}
