package io.enmasse.systemtest.standard.authz;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.authz.AuthorizationTestBase;
import org.junit.Test;

public class AuthorizationTest extends AuthorizationTestBase {
    @Override
    protected AddressSpaceType getAddressSpaceType() {
        return AddressSpaceType.STANDARD;
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
    public void testUserPermissionAfterRemoveAuthz() throws Exception {
        doTestUserPermissionAfterRemoveAuthz();
    }
}
