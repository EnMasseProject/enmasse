/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.auth;

import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.bases.auth.AuthenticationTestBase;
import org.junit.Test;

public class AuthenticationTest extends AuthenticationTestBase {

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
    public void testStandardAuthenticationService() throws Exception {
        testStandardAuthenticationServiceGeneral(AddressSpaceType.STANDARD);
    }

    @Test
    public void testNoneAuthenticationService() throws Exception {
        testNoneAuthenticationServiceGeneral(AddressSpaceType.STANDARD, null, null);
    }
}
