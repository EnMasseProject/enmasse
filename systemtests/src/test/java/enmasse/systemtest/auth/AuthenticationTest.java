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
package enmasse.systemtest.auth;

import enmasse.systemtest.ConnectTimeoutException;
import enmasse.systemtest.Destination;
import enmasse.systemtest.TestBase;
import enmasse.systemtest.amqp.AmqpClient;
import enmasse.systemtest.mqtt.MqttClient;
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

    private List<String> addressSpaces = new ArrayList<>();
    private static final String amqpAddress = "a1";
    private static final String mqttAddress = "t1";

    @Before
    public void setupSpaceList() throws Exception {
        addressSpaces = new ArrayList<>();
    }

    @After
    public void teardownSpaces() throws Exception {
        for (String addressSpace : addressSpaces) {
            deleteAddressSpace(addressSpace);
            getKeycloakClient().deleteUser(addressSpace, "bob");
        }
        addressSpaces.clear();
    }

    @Override
    protected boolean createDefaultAddressSpace() {
        return false;
    }

    public void createAddressSpace(String name, String authService) throws Exception {
        addressSpaces.add(name);
        super.createAddressSpace(name, authService);
        setAddresses(name, Destination.anycast(amqpAddress)); //, Destination.topic(mqttAddress));
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


    private boolean canConnectWithAmqp(String addressSpace, String username, String password) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);
        Future<List<Message>> received = client.recvMessages(amqpAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(amqpAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithMqtt(String addressSpace, String username, String password) throws Exception {
        MqttClient client = mqttClientFactory.createClient(addressSpace);
        MqttConnectOptions options = client.getMqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        Future<List<String>> received = client.recvMessages("t1", 1);
        Future<Integer> sent = client.sendMessages("t1", Arrays.asList("msgt1"));

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

}
