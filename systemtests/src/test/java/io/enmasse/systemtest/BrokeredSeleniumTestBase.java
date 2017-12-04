package io.enmasse.systemtest;

public class BrokeredSeleniumTestBase extends SeleniumTestBase {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }
}
