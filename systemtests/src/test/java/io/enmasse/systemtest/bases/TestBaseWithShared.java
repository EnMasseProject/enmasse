/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.resolvers.ExtensionContextParameterResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag("shared")
public abstract class TestBaseWithShared extends TestBase {
    private static final String defaultAddressTemplate = "-shared-";
    private static final Destination dummyAddress = Destination.queue("dummy-address", "pooled-queue");
    protected static AddressSpace sharedAddressSpace;
    protected static HashMap<String, AddressSpace> sharedAddressSpaces = new HashMap<>();
    private static Logger log = CustomLogger.getLogger();
    private static Map<AddressSpaceType, Integer> spaceCountMap = new HashMap<>();

    protected static void deleteSharedAddressSpace(AddressSpace addressSpace) throws Exception {
        sharedAddressSpaces.remove(addressSpace.getName());
        TestBase.deleteAddressSpace(addressSpace);
    }

    protected abstract AddressSpaceType getAddressSpaceType();

    protected abstract boolean skipDummyAddress();

    public AddressSpace getSharedAddressSpace() {
        return sharedAddressSpace;
    }

    @BeforeEach
    public void setupShared() throws Exception {
        spaceCountMap.putIfAbsent(getAddressSpaceType(), 0);
        sharedAddressSpace = new AddressSpace(
                getAddressSpaceType().name().toLowerCase() + defaultAddressTemplate + spaceCountMap.get(getAddressSpaceType()),
                getAddressSpaceType(),
                AuthService.STANDARD);
        log.info("Test is running in multitenant mode");
        createSharedAddressSpace(sharedAddressSpace);
        if (environment.useDummyAddress() && !skipDummyAddress()) {
            if (!addressExists(dummyAddress)) {
                log.info("'{}' address doesn't exist and will be created", dummyAddress);
                super.setAddresses(sharedAddressSpace, dummyAddress);
            }
        }

        this.username = "test";
        this.password = "test";
        getKeycloakClient().createUser(sharedAddressSpace.getName(), username, password, 1, TimeUnit.MINUTES);

        this.managementCredentials = new KeycloakCredentials("artemis-admin", "artemis-admin");
        getKeycloakClient().createUser(sharedAddressSpace.getName(),
                managementCredentials.getUsername(),
                managementCredentials.getPassword(),
                Group.ADMIN.toString(),
                Group.SEND_ALL_BROKERED.toString(),
                Group.RECV_ALL_BROKERED.toString(),
                Group.VIEW_ALL_BROKERED.toString(),
                Group.MANAGE_ALL_BROKERED.toString());

        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, sharedAddressSpace, username, password);
        mqttClientFactory = new MqttClientFactory(kubernetes, environment, sharedAddressSpace, username, password);
    }

    @AfterEach
    public void tearDownShared(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!environment.skipCleanup()) {
                log.info(String.format("test failed: %s.%s",
                        context.getTestClass().get().getName(),
                        context.getTestMethod().get().getName()));
                log.info("shared address space '{}' will be removed", sharedAddressSpace);
                try {
                    deleteSharedAddressSpace(sharedAddressSpace);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    spaceCountMap.put(sharedAddressSpace.getType(), spaceCountMap.get(sharedAddressSpace.getType()) + 1);
                }
            } else {
                log.warn("Remove address spaces when test failed - SKIPPED!");
            }
        } else { //succeed
            try {
                setAddresses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void createSharedAddressSpace(AddressSpace addressSpace) throws Exception {
        sharedAddressSpaces.put(addressSpace.getName(), addressSpace);
        super.createAddressSpace(addressSpace);
    }

    protected void scale(Destination destination, int numReplicas) throws Exception {
        scale(sharedAddressSpace, destination, numReplicas);
    }

    /**
     * get all addresses except 'dummy-address'
     */
    protected Future<List<String>> getAddresses(Optional<String> addressName) throws Exception {
        return TestUtils.getAddresses(addressApiClient, sharedAddressSpace, addressName, Arrays.asList(dummyAddress.getAddress()));
    }

    /**
     * check if address exists
     */
    protected boolean addressExists(Destination destination) throws Exception {
        Future<List<String>> addresses = TestUtils.getAddresses(addressApiClient, sharedAddressSpace, Optional.empty(),
                new ArrayList<>());
        List<String> address = addresses.get(20, TimeUnit.SECONDS);
        log.info("found addresses");
        address.stream().forEach(addr -> log.info("- address '{}'", addr));
        log.info("looking for '{}' address", destination.getAddress());
        return address.contains(destination.getAddress());
    }

    protected Future<List<Address>> getAddressesObjects(Optional<String> addressName) throws Exception {
        return TestUtils.getAddressesObjects(addressApiClient, sharedAddressSpace, addressName, Arrays.asList(dummyAddress.getAddress()));
    }

    protected Future<List<Destination>> getDestinationsObjects(Optional<String> addressName) throws Exception {
        return TestUtils.getDestinationsObjects(addressApiClient, sharedAddressSpace, addressName, Arrays.asList(dummyAddress.getAddress()));
    }

    /**
     * delete all addresses except 'dummy-address' and append new addresses
     *
     * @param destinations
     * @throws Exception
     */
    protected void setAddresses(Destination... destinations) throws Exception {
        if (isBrokered(sharedAddressSpace) || !environment.useDummyAddress()) {
            setAddresses(sharedAddressSpace, destinations);
        } else {
            List<Destination> inShared = getDestinationsObjects(Optional.empty())
                    .get(10, TimeUnit.SECONDS);
            if (inShared.size() > 0) {
                deleteAddresses(inShared.toArray(new Destination[0]));
            }
            if (destinations.length > 0) {
                appendAddresses(destinations);
            }
        }
    }

    /**
     * append new addresses into address-space and sharedAddresses list
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
    protected List<AbstractClient> attachReceivers(Destination destination, int receiverCount, String
            username, String password) throws Exception {
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
    public void runRestApiTest(Destination d1, Destination d2) throws Exception {
        List<String> destinationsNames = Arrays.asList(d1.getAddress(), d2.getAddress());
        setAddresses(d1);
        appendAddresses(d2);

        //d1, d2
        Future<List<String>> response = getAddresses(Optional.empty());
        assertThat("Rest api does not return all addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames));
        log.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        response = getAddresses(Optional.ofNullable(TestUtils.sanitizeAddress(d2.getName())));
        assertThat("Rest api does not return specific address", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));

        deleteAddresses(d1);

        //d2
        response = getAddresses(Optional.ofNullable(TestUtils.sanitizeAddress(d2.getName())));
        assertThat("Rest api does not return right addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));
        log.info("address {} successfully deleted", d1.getAddress());

        deleteAddresses(d2);

        //empty
        response = getAddresses(Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", d2.getAddress());

        setAddresses(d1, d2);
        deleteAddresses(d1, d2);

        response = getAddresses(Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", Arrays.toString(destinationsNames.toArray()));
    }
}
