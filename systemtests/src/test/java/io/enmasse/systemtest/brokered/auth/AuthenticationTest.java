/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.auth;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.auth.AuthenticationTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class AuthenticationTest extends AuthenticationTestBase {
    private static Logger log = CustomLogger.getLogger();

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

    /**
     * related github issue: #523
     */
    @Test
    public void testStandardAuthenticationServiceRestartBrokered() throws Exception {
        log.info("testStandardAuthenticationServiceRestartBrokered");
        AddressSpace addressSpace = new AddressSpace("keycloak-restart-brokered", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(addressSpace);

        KeycloakCredentials credentials = new KeycloakCredentials("Pavel", "Novak");
        getKeycloakClient().createUser(addressSpace.getName(), credentials.getUsername(), credentials.getPassword());

        assertCanConnect(addressSpace, credentials.getUsername(), credentials.getPassword(), amqpAddressList);

        scaleKeycloak(0);
        scaleKeycloak(1);
        Thread.sleep(160000);

        assertCanConnect(addressSpace, credentials.getUsername(), credentials.getPassword(), amqpAddressList);
    }

    @Test
    public void testStandardAuthenticationServiceBrokered() throws Exception {
        testStandardAuthenticationServiceGeneral(AddressSpaceType.BROKERED);
    }

    @Test
    public void testNoneAuthenticationServiceBrokered() throws Exception {
        testNoneAuthenticationServiceGeneral(AddressSpaceType.BROKERED, anonymousUser, anonymousPswd);
    }
}
