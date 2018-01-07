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
package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class TestBaseWithDefault extends TestBase {
    private static final String defaultAddressTemplate = "-default-";
    protected static AddressSpace defaultAddressSpace;
    protected static HashMap<String, AddressSpace> defaultAddressSpaces = new HashMap<>();
    private static Map<AddressSpaceType, Integer> spaceCountMap = new HashMap<>();
    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            Logging.log.info("test failed:" + description);
            Logging.log.info("default address space '{}' will be removed", defaultAddressSpace);
            try {
                deleteDefaultAddressSpace(defaultAddressSpace);
                spaceCountMap.put(defaultAddressSpace.getType(), spaceCountMap.get(defaultAddressSpace.getType()) + 1);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    protected static void deleteDefaultAddressSpace(AddressSpace addressSpace) throws Exception {
        defaultAddressSpaces.remove(addressSpace.getName());
        TestBase.deleteAddressSpace(addressSpace);
    }

    protected abstract AddressSpaceType getAddressSpaceType();

    public AddressSpace getSharedAddressSpace() {
        return defaultAddressSpace;
    }

    @Before
    public void setupDefault() throws Exception {
        spaceCountMap.putIfAbsent(getAddressSpaceType(), 0);
        defaultAddressSpace = new AddressSpace(getAddressSpaceType().name().toLowerCase() + defaultAddressTemplate + spaceCountMap.get(getAddressSpaceType()), getAddressSpaceType());
        Logging.log.info("Test is running in multitenant mode");
        createDefaultAddressSpace(defaultAddressSpace, "standard");

        this.username = "test";
        this.password = "test";
        getKeycloakClient().createUser(defaultAddressSpace.getName(), username, password, 1, TimeUnit.MINUTES);

        this.managementCredentials = new KeycloakCredentials("artemis-admin", "artemis-admin");
        getKeycloakClient().createUser(defaultAddressSpace.getName(),
                managementCredentials.getUsername(),
                managementCredentials.getPassword());

        createGroup(defaultAddressSpace, "admin");
        joinGroup(defaultAddressSpace, "admin", managementCredentials.getUsername());

        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, defaultAddressSpace, username, password);
        mqttClientFactory = new MqttClientFactory(kubernetes, environment, defaultAddressSpace, username, password);
    }

    @After
    public void teardownDefault() throws Exception {
        setAddresses(defaultAddressSpace);
    }

    protected void createDefaultAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        defaultAddressSpaces.put(addressSpace.getName(), addressSpace);
        super.createAddressSpace(addressSpace, authService);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        scale(defaultAddressSpace, destination, numReplicas);
    }

    protected Future<List<String>> getAddresses(Optional<String> addressName) throws Exception {
        return getAddresses(defaultAddressSpace, addressName);
    }

    /**
     * use PUT html method to replace all addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void setAddresses(Destination... destinations) throws Exception {
        setAddresses(defaultAddressSpace, destinations);
    }

    /**
     * use POST html method to append addresses to already existing addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void appendAddresses(Destination... destinations) throws Exception {
        appendAddresses(defaultAddressSpace, destinations);
    }

    /**
     * use DELETE html method for delete specific addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void deleteAddresses(Destination... destinations) throws Exception {
        deleteAddresses(defaultAddressSpace, destinations);
    }

    /**
     * attach N receivers into one address with default username/password
     */
    protected List<AbstractClient> attachReceivers(Destination destination, int receiverCount) throws Exception {
        return attachReceivers(defaultAddressSpace, destination, receiverCount, username, password);
    }

    /**
     * attach N receivers into one address with own username/password
     */
    protected List<AbstractClient> attachReceivers(Destination destination, int receiverCount, String username, String password) throws Exception {
        return attachReceivers(defaultAddressSpace, destination, receiverCount, username, password);
    }

    /**
     * attach senders to destinations
     */
    protected List<AbstractClient> attachSenders(List<Destination> destinations) throws Exception {
        return attachSenders(defaultAddressSpace, destinations);
    }

    /**
     * attach receivers to destinations
     */
    protected List<AbstractClient> attachReceivers(List<Destination> destinations) throws Exception {
        return attachReceivers(defaultAddressSpace, destinations);
    }

    /**
     * create M connections with N receivers and K senders
     */
    protected AbstractClient attachConnector(Destination destination, int connectionCount,
                                             int senderCount, int receiverCount) throws Exception {
        return attachConnector(defaultAddressSpace, destination, connectionCount, senderCount, receiverCount);
    }

    /**
     * body for rest api tests
     */
    public void runRestApiTest(List<String> destinationsNames, Destination d1, Destination d2) throws Exception {
        setAddresses(d1);
        appendAddresses(d2);

        //queue1, queue2
        Future<List<String>> response = getAddresses(Optional.empty());
        assertThat(response.get(1, TimeUnit.MINUTES), is(destinationsNames));
        Logging.log.info("addresses (" + destinationsNames.stream().collect(Collectors.joining(",")) + ") successfully created");

        deleteAddresses(d1);

        //queue1
        response = getAddresses(Optional.empty());
        assertThat(response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));
        Logging.log.info("address (" + d1.getAddress() + ") successfully deleted");

        deleteAddresses(d2);

        //empty
        response = getAddresses(Optional.empty());
        assertThat(response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        Logging.log.info("addresses (" + d2.getAddress() + ") successfully deleted");
    }

}

