/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.authz;

import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.authz.AuthorizationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AuthorizationTest extends AuthorizationTestBase implements ITestBaseBrokered {

    @Test
    void testSendAuthz() throws Exception {
        doTestSendAuthz();
    }

    @Test
    void testReceiveAuthz() throws Exception {
        doTestReceiveAuthz();
    }

    @Test
    @Disabled("disabled due to issue #786")
    void testUserPermissionAfterRemoveAuthz() throws Exception {
        doTestUserPermissionAfterRemoveAuthz();
    }

    @Test
    void testSendAuthzWithWIldcards() throws Exception {
        doTestSendAuthzWithWIldcards();
    }

    @Test
    void testReceiveAuthzWithWIldcards() throws Exception {
        doTestReceiveAuthzWithWIldcards();
    }
}
