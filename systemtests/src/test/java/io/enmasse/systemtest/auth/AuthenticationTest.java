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
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.mqtt.MqttClient;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AuthenticationTest extends TestBase {

    private List<AddressSpace> addressSpaces = new ArrayList<>();
    private static final List<Destination> amqpAddressList = Arrays.asList(
            Destination.queue("auth-queue"),
            Destination.topic("auth-topic"),
            Destination.anycast("auth-anycast"),
            Destination.multicast("auth-multicast"));
    private static final String mqttAddress = "t1";
    private static final String anonymousUser = "anonymous";
    private static final String anonymousPswd = "anonymous";

    @Before
    public void setupSpaceList() throws Exception {
        addressSpaces = new ArrayList<>();
    }

    @After
    public void teardownSpaces() throws Exception {
        for (AddressSpace addressSpace : addressSpaces) {
            getKeycloakClient().deleteUser(addressSpace.getName(), "bob");
            deleteAddressSpace(addressSpace);
        }
        addressSpaces.clear();
    }

    @Override
    protected boolean createDefaultAddressSpace() {
        return false;
    }

    @Override
    protected AddressSpace createAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        super.createAddressSpace(addressSpace, authService);
        addressSpaces.add(addressSpace);
        List<Destination> brokeredAddressList = new ArrayList<>(amqpAddressList);
        if (addressSpace.getType().equals(AddressSpaceType.BROKERED)) {
            brokeredAddressList = amqpAddressList.subList(0, 2);
        }
        setAddresses(addressSpace, brokeredAddressList.toArray(new Destination[brokeredAddressList.size()]));
        //        setAddresses(name, Destination.queue(amqpAddress)); //, Destination.topic(mqttAddress)); #TODO! for MQTT
        return addressSpace;
    }

    @Test
    public void testStandardAuthenticationServiceRestart() throws Exception{
        Logging.log.info("testStandardAuthenticationServiceRestart");
        scaleInGlobal("keycloak", 5);
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
        assertCanConnect(s3standard, emptyUser, emptyPassword);
        assertCanConnect(s3standard, "bob", "pass");

        AddressSpace s4standard = new AddressSpace(type.toString().toLowerCase() + "-s4", type);
        createAddressSpace(s4standard, "standard");
        assertCanConnect(s3standard, emptyUser, emptyPassword);
        assertCanConnect(s3standard, "bob", "pass");
        assertCannotConnect(s4standard, emptyUser, emptyPassword);
        assertCannotConnect(s4standard, "bob", "pass");
    }

    public void testStandardAuthenticationServiceGeneral(AddressSpaceType type) throws Exception {
        AddressSpace s1brokered = new AddressSpace(type.toString().toLowerCase() + "-s1", type);
        createAddressSpace(s1brokered, "standard");

        // Validate unsuccessful authentication with enmasse authentication service with no credentials
        assertCannotConnect(s1brokered, null, null);
        assertCannotConnect(s1brokered, "bob", "s1pass");

        KeycloakCredentials s1Bob = new KeycloakCredentials("bob", "s1pass");
        getKeycloakClient().createUser(s1brokered.getName(), s1Bob.getUsername(), s1Bob.getPassword());

        KeycloakCredentials s1Carol = new KeycloakCredentials("carol", "s2pass");
        getKeycloakClient().createUser(s1brokered.getName(), s1Carol.getUsername(), s1Carol.getPassword());

        assertCannotConnect(s1brokered, null, null);

        // Validate successful authentication with enmasse authentication service and valid credentials
        assertCanConnect(s1brokered, s1Bob.getUsername(), s1Bob.getPassword());
        assertCanConnect(s1brokered, s1Carol.getUsername(), s1Carol.getPassword());

        // Validate unsuccessful authentication with enmasse authentication service with incorrect credentials
        assertCannotConnect(s1brokered, s1Bob.getUsername(), "s2pass");
        assertCannotConnect(s1brokered, "alice", "s1pass");

        AddressSpace s2brokered = new AddressSpace(type.toString().toLowerCase() + "-s2", type);
        createAddressSpace(s2brokered, "standard");

        KeycloakCredentials s2Bob = new KeycloakCredentials("bob", "s2pass");
        getKeycloakClient().createUser(s2brokered.getName(), s2Bob.getUsername(), s2Bob.getPassword());

        //create user with the same credentials in different address spaces
        KeycloakCredentials s2Carol = new KeycloakCredentials("carol", "s2pass");
        getKeycloakClient().createUser(s2brokered.getName(), s1Carol.getUsername(), s1Carol.getPassword());

        assertCanConnect(s1brokered, s1Bob.getUsername(), s1Bob.getPassword());
        assertCanConnect(s1brokered, s2Carol.getUsername(), s2Carol.getPassword());
        assertCanConnect(s2brokered, s2Bob.getUsername(), s2Bob.getPassword());
        assertCanConnect(s2brokered, s2Carol.getUsername(), s2Carol.getPassword());

        assertCannotConnect(s2brokered, s1Bob.getUsername(), s1Bob.getPassword());
        assertCannotConnect(s1brokered, s2Bob.getUsername(), s2Bob.getPassword());
    }

    private void assertCanConnect(AddressSpace addressSpace, String username, String password) throws Exception {
        assertTrue(canConnectWithAmqp(addressSpace, username, password));
        // TODO: Enable this when mqtt is stable enough
        // assertTrue(canConnectWithMqtt(addressSpace, username, password));
    }

    private void assertCannotConnect(AddressSpace addressSpace, String username, String password) throws Exception {
        try {
            assertFalse(canConnectWithAmqp(addressSpace, username, password));
            fail("Expected connection to timeout");
        } catch (ConnectTimeoutException e) {
        }

        // TODO: Enable this when mqtt is stable enough
        // assertFalse(canConnectWithMqtt(addressSpace, username, password));
    }


    private boolean canConnectWithAmqp(AddressSpace addressSpace, String username, String password) throws Exception {
        assertTrue(canConnectWithAmqpToQueue(addressSpace, username, password, amqpAddressList.get(0).getAddress()));
        assertTrue(canConnectWithAmqpToTopic(addressSpace, username, password, amqpAddressList.get(1).getAddress()));
        if (!TestUtils.getAddressSpaceType(getAddressSpace(addressSpace.getName())).equals("brokered")) {
            assertTrue(canConnectWithAmqpToAnycast(addressSpace, username, password, amqpAddressList.get(2).getAddress()));
            assertTrue(canConnectWithAmqpToMulticast(addressSpace, username, password, amqpAddressList.get(3).getAddress()));
        }
        return true;
    }

    private boolean canConnectWithAmqpToQueue(AddressSpace addressSpace, String username, String password, String queueAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<Integer> sent = client.sendMessages(queueAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);
        Future<List<Message>> received = client.recvMessages(queueAddress, 1, 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToAnycast(AddressSpace addressSpace, String username, String password, String anycastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(anycastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(anycastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToMulticast(AddressSpace addressSpace, String username, String password, String multicastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createBroadcastClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(multicastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(multicastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToTopic(AddressSpace addressSpace, String username, String password, String topicAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createTopicClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(topicAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(topicAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithMqtt(String name, String username, String password) throws Exception {
        AddressSpace addressSpace = new AddressSpace(name);
        MqttClient client = mqttClientFactory.createClient(addressSpace);
        MqttConnectOptions options = client.getMqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        Future<List<String>> received = client.recvMessages("t1", 1);
        Future<Integer> sent = client.sendMessages("t1", Arrays.asList("msgt1"));

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

}
