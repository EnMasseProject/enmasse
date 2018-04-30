/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.auth;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.auth.AuthenticationTestBase;
import org.junit.jupiter.api.Test;

class AuthenticationTest extends AuthenticationTestBase implements ITestBaseStandard {

    @Test
    void testStandardAuthenticationService() throws Exception {
        testStandardAuthenticationServiceGeneral(AddressSpaceType.STANDARD);
    }

    @Test
    void testNoneAuthenticationService() throws Exception {
        testNoneAuthenticationServiceGeneral(AddressSpaceType.STANDARD, null, null);
    }
}
