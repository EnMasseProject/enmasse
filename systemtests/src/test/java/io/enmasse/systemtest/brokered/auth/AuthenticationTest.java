/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.systemtest.brokered.auth;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.auth.AuthenticationTestBase;
import org.junit.Test;

public class AuthenticationTest extends AuthenticationTestBase {
    /**
     * related github issue: #523
     */
    @Test
    public void testStandardAuthenticationServiceRestartBrokered() throws Exception {
        Logging.log.info("testStandardAuthenticationServiceRestartBrokered");
        AddressSpace addressSpace = new AddressSpace("keycloak-restart-brokered", AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "standard");

        KeycloakCredentials credentials = new KeycloakCredentials("Pavel", "Novak");
        getKeycloakClient().createUser(addressSpace.getName(), credentials.getUsername(), credentials.getPassword());

        assertCanConnect(addressSpace, credentials.getUsername(), credentials.getPassword(), amqpAddressList);

        scaleKeycloak(0);
        scaleKeycloak(1);
        Thread.sleep(60000);

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
