package io.enmasse.systemtest.brokered.clients;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.bases.clients.MsgPatternsTestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class MsgPatternsTest extends MsgPatternsTestBase {
    @Override
    protected String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return "brokered-queue";
            case TOPIC:
                return "brokered-topic";
        }
        return null;
    }
}
