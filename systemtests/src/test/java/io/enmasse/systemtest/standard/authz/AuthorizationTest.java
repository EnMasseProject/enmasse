/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.authz;

import io.enmasse.systemtest.bases.ITestBaseStandard;
import io.enmasse.systemtest.bases.authz.AuthorizationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AuthorizationTest extends AuthorizationTestBase implements ITestBaseStandard {

    @Test
    public void testSendAuthz() throws Exception {
        doTestSendAuthz();
    }

    @Test
    public void testReceiveAuthz() throws Exception {
        doTestReceiveAuthz();
    }

    @Test
    @Disabled("related issue: #786")
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
