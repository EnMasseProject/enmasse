/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category(SharedAddressSpace.class)
public abstract class TestBaseWithShared extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private static final String defaultAddressTemplate = "-shared-";
    protected static AddressSpace sharedAddressSpace;
    protected static HashMap<String, AddressSpace> sharedAddressSpaces = new HashMap<>();
    private static Map<AddressSpaceType, Integer> spaceCountMap = new HashMap<>();
    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            log.info("test failed:" + description);
            log.info("shared address space '{}' will be removed", sharedAddressSpace);
            try {
                deleteSharedAddressSpace(sharedAddressSpace);
                spaceCountMap.put(sharedAddressSpace.getType(), spaceCountMap.get(sharedAddressSpace.getType()) + 1);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    protected static void deleteSharedAddressSpace(AddressSpace addressSpace) throws Exception {
        sharedAddressSpaces.remove(addressSpace.getName());
        TestBase.deleteAddressSpace(addressSpace);
    }

    protected abstract AddressSpaceType getAddressSpaceType();

    public AddressSpace getSharedAddressSpace() {
        return sharedAddressSpace;
    }

    @Before
    public void setupShared() throws Exception {
        spaceCountMap.putIfAbsent(getAddressSpaceType(), 0);
        sharedAddressSpace = new AddressSpace(getAddressSpaceType().name().toLowerCase() + defaultAddressTemplate + spaceCountMap.get(getAddressSpaceType()), getAddressSpaceType());
        log.info("Test is running in multitenant mode");
        createSharedAddressSpace(sharedAddressSpace, "standard");

        this.username = "test";
        this.password = "test";
        getKeycloakClient().createUser(sharedAddressSpace.getName(), username, password, 1, TimeUnit.MINUTES);

        this.managementCredentials = new KeycloakCredentials("artemis-admin", "artemis-admin");
        getKeycloakClient().createUser(sharedAddressSpace.getName(),
                managementCredentials.getUsername(),
                managementCredentials.getPassword(),
                "admin", "send_#", "recv_#", "view_#", "manage_#");

        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, sharedAddressSpace, username, password);
        mqttClientFactory = new MqttClientFactory(kubernetes, environment, sharedAddressSpace, username, password);
    }

    @After
    public void teardownShared() throws Exception {
        setAddresses(sharedAddressSpace);
    }

    protected void createSharedAddressSpace(AddressSpace addressSpace, String authService) throws Exception {
        sharedAddressSpaces.put(addressSpace.getName(), addressSpace);
        super.createAddressSpace(addressSpace, authService);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        scale(sharedAddressSpace, destination, numReplicas);
    }

    protected Future<List<String>> getAddresses(Optional<String> addressName) throws Exception {
        return getAddresses(sharedAddressSpace, addressName);
    }

    protected Future<List<Address>> getAddressesObjects(Optional<String> addressName) throws Exception {
        return getAddressesObjects(sharedAddressSpace, addressName);
    }


    /**
     * use PUT html method to replace all addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void setAddresses(Destination... destinations) throws Exception {
        setAddresses(sharedAddressSpace, destinations);
    }

    /**
     * use POST html method to append addresses to already existing addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void appendAddresses(Destination... destinations) throws Exception {
        appendAddresses(sharedAddressSpace, destinations);
    }

    /**
     * use DELETE html method for delete specific addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void deleteAddresses(Destination... destinations) throws Exception {
        deleteAddresses(sharedAddressSpace, destinations);
    }

    /**
     * attach N receivers into one address with default username/password
     */
    protected List<AbstractClient> attachReceivers(Destination destination, int receiverCount) throws Exception {
        return attachReceivers(sharedAddressSpace, destination, receiverCount, username, password);
    }

    /**
     * attach N receivers into one address with own username/password
     */
    protected List<AbstractClient> attachReceivers(Destination destination, int receiverCount, String username, String password) throws Exception {
        return attachReceivers(sharedAddressSpace, destination, receiverCount, username, password);
    }

    /**
     * attach senders to destinations
     */
    protected List<AbstractClient> attachSenders(List<Destination> destinations) throws Exception {
        return attachSenders(sharedAddressSpace, destinations);
    }

    /**
     * attach receivers to destinations
     */
    protected List<AbstractClient> attachReceivers(List<Destination> destinations) throws Exception {
        return attachReceivers(sharedAddressSpace, destinations);
    }

    /**
     * create M connections with N receivers and K senders
     */
    protected AbstractClient attachConnector(Destination destination, int connectionCount,
                                             int senderCount, int receiverCount) throws Exception {
        return attachConnector(sharedAddressSpace, destination, connectionCount, senderCount, receiverCount, username, password);
    }

    /**
     * body for rest api tests
     */
    public void runRestApiTest(List<String> destinationsNames, Destination d1, Destination d2) throws Exception {
        setAddresses(d1);
        appendAddresses(d2);

        //queue1, queue2
        Future<List<String>> response = getAddresses(Optional.empty());
        assertThat("Rest api does not return all addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames));
        log.info("addresses (" + destinationsNames.stream().collect(Collectors.joining(",")) + ") successfully created");

        deleteAddresses(d1);

        //queue1
        response = getAddresses(Optional.empty());
        assertThat("Rest api does not return right addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));
        log.info("address (" + d1.getAddress() + ") successfully deleted");

        deleteAddresses(d2);

        //empty
        response = getAddresses(Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses (" + d2.getAddress() + ") successfully deleted");
    }
}
