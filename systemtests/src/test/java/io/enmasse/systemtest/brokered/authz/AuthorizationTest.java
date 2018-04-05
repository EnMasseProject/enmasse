/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.authz;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.bases.authz.AuthorizationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AuthorizationTest extends AuthorizationTestBase {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.BROKERED;
    }

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

    @Override
    protected boolean skipDummyAddress() {
        return true;
    }

    @Test
    public void testSendAuthz() throws Exception {
        doTestSendAuthz();
    }

    @Test
    public void testReceiveAuthz() throws Exception {
        doTestReceiveAuthz();
    }

    @Test
    @Disabled("disabled due to issue #786")
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
