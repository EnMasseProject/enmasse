package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.web.WebConsoleTest;

public abstract class BrokeredWebConsoleTest extends WebConsoleTest {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }
}
