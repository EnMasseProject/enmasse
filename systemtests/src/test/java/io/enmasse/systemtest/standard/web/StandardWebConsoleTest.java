package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.web.WebConsoleTest;
import org.junit.Ignore;

@Ignore
public abstract class StandardWebConsoleTest extends WebConsoleTest {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
    }

    @Override
    protected String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return "sharded-queue";
            case TOPIC:
                return "sharded-topic";
            case ANYCAST:
                return "standard-anycast";
            case MULTICAST:
                return "standard-multicast";
        }
        return null;
    }
}
