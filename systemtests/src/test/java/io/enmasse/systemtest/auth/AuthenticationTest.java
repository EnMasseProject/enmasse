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
package io.enmasse.systemtest.auth;

import io.enmasse.systemtest.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthenticationTest extends TestBase {

    private static final List<Destination> amqpAddressList = Arrays.asList(
            Destination.queue("auth-queue"),
            Destination.topic("auth-topic"),
            Destination.anycast("auth-anycast"),
            Destination.multicast("auth-multicast"));
    private static final String mqttAddress = "t1";
    private static final String anonymousUser = "anonymous";
    private static final String anonymousPswd = "anonymous";

    @Override
    protected void createAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        super.createAddressSpace(addressSpace, authService);
        List<Destination> brokeredAddressList = new ArrayList<>(amqpAddressList);
        if (addressSpace.getType().equals(AddressSpaceType.BROKERED)) {
            brokeredAddressList = amqpAddressList.subList(0, 2);
        }
        setAddresses(addressSpace, brokeredAddressList.toArray(new Destination[brokeredAddressList.size()]));
        //        setAddresses(name, Destination.queue(amqpAddress)); //, Destination.topic(mqttAddress)); #TODO! for MQTT
    }


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

    @Test
    public void testStandardAuthenticationService() throws Exception {
        testStandardAuthenticationServiceGeneral(AddressSpaceType.STANDARD);
    }

    @Test
    public void testNoneAuthenticationService() throws Exception {
        testNoneAuthenticationServiceGeneral(AddressSpaceType.STANDARD, null, null);
    }

    public void testNoneAuthenticationServiceGeneral(AddressSpaceType type, String emptyUser, String emptyPassword) throws Exception {
        AddressSpace s3standard = new AddressSpace(type.toString().toLowerCase() + "-s3", type);
        createAddressSpace(s3standard, "none");
        assertCanConnect(s3standard, emptyUser, emptyPassword, amqpAddressList);
        assertCanConnect(s3standard, "bob", "pass", amqpAddressList);

        AddressSpace s4standard = new AddressSpace(type.toString().toLowerCase() + "-s4", type);
        createAddressSpace(s4standard, "standard");
        assertCanConnect(s3standard, emptyUser, emptyPassword, amqpAddressList);
        assertCanConnect(s3standard, "bob", "pass", amqpAddressList);
        assertCannotConnect(s4standard, emptyUser, emptyPassword, amqpAddressList);
        assertCannotConnect(s4standard, "bob", "pass", amqpAddressList);
    }

    public void testStandardAuthenticationServiceGeneral(AddressSpaceType type) throws Exception {
        AddressSpace s1brokered = new AddressSpace(type.toString().toLowerCase() + "-s1", type);
        createAddressSpace(s1brokered, "standard");

        // Validate unsuccessful authentication with enmasse authentication service with no credentials
        assertCannotConnect(s1brokered, null, null, amqpAddressList);
        assertCannotConnect(s1brokered, "bob", "s1pass", amqpAddressList);

        KeycloakCredentials s1Bob = new KeycloakCredentials("bob", "s1pass");
        getKeycloakClient().createUser(s1brokered.getName(), s1Bob.getUsername(), s1Bob.getPassword());

        KeycloakCredentials s1Carol = new KeycloakCredentials("carol", "s2pass");
        getKeycloakClient().createUser(s1brokered.getName(), s1Carol.getUsername(), s1Carol.getPassword());

        assertCannotConnect(s1brokered, null, null, amqpAddressList);

        // Validate successful authentication with enmasse authentication service and valid credentials
        assertCanConnect(s1brokered, s1Bob.getUsername(), s1Bob.getPassword(), amqpAddressList);
        assertCanConnect(s1brokered, s1Carol.getUsername(), s1Carol.getPassword(), amqpAddressList);

        // Validate unsuccessful authentication with enmasse authentication service with incorrect credentials
        assertCannotConnect(s1brokered, s1Bob.getUsername(), "s2pass", amqpAddressList);
        assertCannotConnect(s1brokered, "alice", "s1pass", amqpAddressList);

        AddressSpace s2brokered = new AddressSpace(type.toString().toLowerCase() + "-s2", type);
        createAddressSpace(s2brokered, "standard");

        KeycloakCredentials s2Bob = new KeycloakCredentials("bob", "s2pass");
        getKeycloakClient().createUser(s2brokered.getName(), s2Bob.getUsername(), s2Bob.getPassword());

        //create user with the same credentials in different address spaces
        KeycloakCredentials s2Carol = new KeycloakCredentials("carol", "s2pass");
        getKeycloakClient().createUser(s2brokered.getName(), s1Carol.getUsername(), s1Carol.getPassword());

        assertCanConnect(s1brokered, s1Bob.getUsername(), s1Bob.getPassword(), amqpAddressList);
        assertCanConnect(s1brokered, s2Carol.getUsername(), s2Carol.getPassword(), amqpAddressList);
        assertCanConnect(s2brokered, s2Bob.getUsername(), s2Bob.getPassword(), amqpAddressList);
        assertCanConnect(s2brokered, s2Carol.getUsername(), s2Carol.getPassword(), amqpAddressList);

        assertCannotConnect(s2brokered, s1Bob.getUsername(), s1Bob.getPassword(), amqpAddressList);
        assertCannotConnect(s1brokered, s2Bob.getUsername(), s2Bob.getPassword() ,amqpAddressList);
    }

}
