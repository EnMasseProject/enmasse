package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.web.WebConsoleTest;

public abstract class StandardWebConsoleTest extends WebConsoleTest {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }
}
