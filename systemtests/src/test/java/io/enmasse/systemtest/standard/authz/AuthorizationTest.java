/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.authz;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.bases.authz.AuthorizationTestBase;
import org.junit.Test;

public class AuthorizationTest extends AuthorizationTestBase {
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

    @Test
    public void testSendAuthz() throws Exception {
        doTestSendAuthz();
    }

    @Test
    public void testReceiveAuthz() throws Exception {
        doTestReceiveAuthz();
    }

    //@Test disabled due to issue #786
    public void testUserPermissionAfterRemoveAuthz() throws Exception {
        doTestUserPermissionAfterRemoveAuthz();
    }

    @Test
    public void testSendAuthzWithWIldcards() throws Exception {
        doTestSendAuthzWithWIldcards();
    }

    @Test
    public void testReceiveAuthzWithWIldcards() throws Exception {
        doTestReceiveAuthzWithWIldcards();
    }
}
