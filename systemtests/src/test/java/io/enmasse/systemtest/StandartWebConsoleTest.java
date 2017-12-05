package io.enmasse.systemtest;

public abstract class StandartWebConsoleTest extends WebConsoleTest {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }
}
