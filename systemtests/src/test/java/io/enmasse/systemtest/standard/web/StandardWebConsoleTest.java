/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.web;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.bases.web.WebConsoleTest;

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
