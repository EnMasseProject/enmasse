/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.auth;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Tag("isolated")
public abstract class AuthenticationTestBase extends TestBase {

    protected final List<Destination> amqpAddressList = Arrays.asList(
            Destination.queue("auth-queue", getDefaultPlan(AddressType.QUEUE)),
            Destination.topic("auth-topic", getDefaultPlan(AddressType.TOPIC)),
            Destination.anycast("auth-anycast"),
            Destination.multicast("auth-multicast"));
    protected static final String mqttAddress = "t1";
    protected static final String anonymousUser = "anonymous";
    protected static final String anonymousPswd = "anonymous";

    @Override
    protected void createAddressSpace(AddressSpace addressSpace) throws Exception {
        super.createAddressSpace(addressSpace);
        List<Destination> brokeredAddressList = new ArrayList<>(amqpAddressList);
        if (addressSpace.getType().equals(AddressSpaceType.BROKERED)) {
            brokeredAddressList = amqpAddressList.subList(0, 2);
        }
        setAddresses(addressSpace, brokeredAddressList.toArray(new Destination[brokeredAddressList.size()]));
        //        setAddresses(name, Destination.queue(amqpAddress)); //, Destination.topic(mqttAddress)); #TODO! for MQTT
    }

    @Override
    protected void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        super.createAddressSpaceList(addressSpaces);
        List<Destination> brokeredAddressList = new ArrayList<>(amqpAddressList);
        for (AddressSpace addressSpace : addressSpaces) {
            if (addressSpace.getType().equals(AddressSpaceType.BROKERED)) {
                brokeredAddressList = amqpAddressList.subList(0, 2);
            }
            setAddresses(addressSpace, brokeredAddressList.toArray(new Destination[brokeredAddressList.size()]));
            //        setAddresses(name, Destination.queue(amqpAddress)); //, Destination.topic(mqttAddress)); #TODO! for MQTT
        }

    }

    protected void testNoneAuthenticationServiceGeneral(AddressSpaceType type, String emptyUser, String emptyPassword) throws Exception {
        AddressSpace s3standard = new AddressSpace(type.toString().toLowerCase() + "-s3", type);
        AddressSpace s4standard = new AddressSpace(type.toString().toLowerCase() + "-s4", type, AuthService.STANDARD);
        createAddressSpaceList(s3standard, s4standard);

        assertCanConnect(s3standard, emptyUser, emptyPassword, amqpAddressList);
        assertCanConnect(s3standard, "bob", "pass", amqpAddressList);

        assertCanConnect(s3standard, emptyUser, emptyPassword, amqpAddressList);
        assertCanConnect(s3standard, "bob", "pass", amqpAddressList);
        assertCannotConnect(s4standard, emptyUser, emptyPassword, amqpAddressList);
        assertCannotConnect(s4standard, "bob", "pass", amqpAddressList);
    }

    protected void testStandardAuthenticationServiceGeneral(AddressSpaceType type) throws Exception {
        AddressSpace s1brokered = new AddressSpace(type.toString().toLowerCase() + "-s1", type, AuthService.STANDARD);
        AddressSpace s2brokered = new AddressSpace(type.toString().toLowerCase() + "-s2", type, AuthService.STANDARD);
        createAddressSpaceList(s1brokered, s2brokered);

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
        assertCannotConnect(s1brokered, s2Bob.getUsername(), s2Bob.getPassword(), amqpAddressList);
    }

}
