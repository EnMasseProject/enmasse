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

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.ConnectTimeoutException;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.TestUtils;
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

    @Before
    public void setupSpaceList() throws Exception {
        addressSpaces = new ArrayList<>();
    }

    @After
    public void teardownSpaces() throws Exception {
        for (AddressSpace addressSpace : addressSpaces) {
            deleteAddressSpace(addressSpace);
            getKeycloakClient().deleteUser(addressSpace.getName(), "bob");
        }
        addressSpaces.clear();
    }

    @Override
    protected boolean createDefaultAddressSpace() {
        return false;
    }

    public void createAddressSpace(String name, String authService) throws Exception {
        AddressSpace addressSpace = new AddressSpace(name);
        addressSpaces.add(addressSpace);
        super.createAddressSpace(addressSpace, authService);
        setAddresses(addressSpace, amqpAddressList.toArray(new Destination[amqpAddressList.size()]));
//        setAddresses(name, Destination.queue(amqpAddress)); //, Destination.topic(mqttAddress));
    }

    public String createAddressSpace(String name, String authService, String type) throws Exception {
        AddressSpace addressSpace = new AddressSpace(name);
        addressSpaces.add(addressSpace);
        super.createAddressSpace(addressSpace, authService, type);
        List<Destination> brokeredAddressList = amqpAddressList.subList(0, 2);
        setAddresses(addressSpace, brokeredAddressList.toArray(new Destination[brokeredAddressList.size()]));
        return addressSpace.getName();
    }

    @Test
    public void testStandardAuthenticationServiceBrokered() throws Exception {
        String s1brokered = createAddressSpace("brokered-s1", "standard", "brokered");

        // Validate unsuccessful authentication with enmasse authentication service with no credentials
        assertCannotConnect(s1brokered, null, null);
        assertCannotConnect(s1brokered, "bob", "s1pass");

        getKeycloakClient().createUser(s1brokered, "bob", "s1pass");
        assertCannotConnect(s1brokered, null, null);

        // Validate successful authentication with enmasse authentication service and valid credentials
        assertCanConnect(s1brokered, "bob", "s1pass");

        // Validate unsuccessful authentication with enmasse authentication service with incorrect credentials
        assertCannotConnect(s1brokered, "bob", "s2pass");
        assertCannotConnect(s1brokered, "alice", "s1pass");

        String s2brokered = createAddressSpace("brokered-s2", "standard", "brokered");

        getKeycloakClient().createUser(s2brokered, "bob", "s2pass");
        assertCanConnect(s1brokered, "bob", "s1pass");
        assertCanConnect(s2brokered, "bob", "s2pass");
        assertCannotConnect(s2brokered, "bob", "s1pass");
        assertCannotConnect(s1brokered, "bob", "s2pass");
    }

    @Test
    public void testNoneAuthenticationServiceBrokered() throws Exception {
        String s3brokered = createAddressSpace("brokered-s3", "none", "brokered");
        assertCanConnect(s3brokered, "", "");
        assertCanConnect(s3brokered, "bob", "pass");

        String s4brokered = createAddressSpace("brokered-s4", "standard", "brokered");
        assertCanConnect(s3brokered, "", "");
        assertCanConnect(s3brokered, "bob", "pass");
        assertCannotConnect(s4brokered, "", "");
        assertCannotConnect(s4brokered, "bob", "pass");
    }

    @Test
    public void testStandardAuthenticationService() throws Exception {
        createAddressSpace("s1", "standard");

        // Validate unsuccessful authentication with enmasse authentication service with no credentials
        assertCannotConnect("s1", null, null);
        assertCannotConnect("s1", "bob", "s1pass");

        getKeycloakClient().createUser("s1", "bob", "s1pass");
        assertCannotConnect("s1", null, null);

        // Validate successful authentication with enmasse authentication service and valid credentials
        assertCanConnect("s1", "bob", "s1pass");

        // Validate unsuccessful authentication with enmasse authentication service with incorrect credentials
        assertCannotConnect("s1", "bob", "s2pass");
        assertCannotConnect("s1", "alice", "s1pass");

        createAddressSpace("s2", "standard");

        getKeycloakClient().createUser("s2", "bob", "s2pass");
        assertCanConnect("s1", "bob", "s1pass");
        assertCanConnect("s2", "bob", "s2pass");
        assertCannotConnect("s2", "bob", "s1pass");
        assertCannotConnect("s1", "bob", "s2pass");
    }

    @Test
    public void testNoneAuthenticationService() throws Exception {
        createAddressSpace("s3", "none");
        assertCanConnect("s3", null, null);
        assertCanConnect("s3", "bob", "pass");

        createAddressSpace("s4", "standard");
        assertCanConnect("s3", null, null);
        assertCanConnect("s3", "bob", "pass");
        assertCannotConnect("s4", null, null);
        assertCannotConnect("s4", "bob", "pass");
    }

    private void assertCanConnect(String addressSpace, String username, String password) throws Exception {
        assertTrue(canConnectWithAmqp(addressSpace, username, password));
        // TODO: Enable this when mqtt is stable enough
        // assertTrue(canConnectWithMqtt(addressSpace, username, password));
    }

    private void assertCannotConnect(String addressSpace, String username, String password) throws Exception {
        try {
            assertFalse(canConnectWithAmqp(addressSpace, username, password));
            fail("Expected connection to timeout");
        } catch (ConnectTimeoutException e) {
        }

        // TODO: Enable this when mqtt is stable enough
        // assertFalse(canConnectWithMqtt(addressSpace, username, password));
    }


        private boolean canConnectWithAmqp (String addressSpace, String username, String password) throws
        InterruptedException, IOException, TimeoutException, ExecutionException {
            assertTrue(canConnectWithAmqpToQueue(addressSpace, username, password, amqpAddressList.get(0).getAddress()));
            assertTrue(canConnectWithAmqpToTopic(addressSpace, username, password, amqpAddressList.get(1).getAddress()));
            if (!TestUtils.getAddressSpaceType(getAddressSpace(addressSpace)).equals("brokered")) {
                assertTrue(canConnectWithAmqpToAnycast(addressSpace, username, password, amqpAddressList.get(2).getAddress()));
                assertTrue(canConnectWithAmqpToMulticast(addressSpace, username, password, amqpAddressList.get(3).getAddress()));
            }
            return true;
        }

    private boolean canConnectWithAmqpToQueue(String addressSpace, String username, String password, String queueAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<Integer> sent = client.sendMessages(queueAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);
        Future<List<Message>> received = client.recvMessages(queueAddress, 1, 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToAnycast(String addressSpace, String username, String password, String anycastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(anycastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(anycastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToMulticast(String addressSpace, String username, String password, String multicastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createBroadcastClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(multicastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(multicastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToTopic(String addressSpace, String username, String password, String topicAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
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
