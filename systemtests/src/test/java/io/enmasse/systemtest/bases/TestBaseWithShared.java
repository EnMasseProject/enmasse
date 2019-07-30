/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.SharedAddressSpaceManager;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TestBaseWithShared extends TestBase {
    private static final String defaultAddressTemplate = "-shared-";
    protected static AddressSpace sharedAddressSpace;
    private static Logger log = CustomLogger.getLogger();
    private static Map<String, Integer> spaceCountMap = new HashMap<>();

    private void deleteSharedAddressSpace(AddressSpace addressSpace) throws Exception {
        deleteAddressSpace(addressSpace);
    }

    public AddressSpace getSharedAddressSpace() {
        return sharedAddressSpace;
    }

    @BeforeEach
    public void setupShared() throws Exception {
        spaceCountMap.putIfAbsent(getDefaultAddrSpaceIdentifier(), 0);
        sharedAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(getDefaultAddrSpaceIdentifier() + defaultAddressTemplate + spaceCountMap.get(getDefaultAddrSpaceIdentifier()))
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(getAddressSpaceType().toString())
                .withPlan(getDefaultAddressSpacePlan())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpace(sharedAddressSpace);
        SharedAddressSpaceManager.getInstance().setActualSharedAddressSpace(sharedAddressSpace);
        defaultCredentials.setUsername("test").setPassword("test");
        createOrUpdateUser(sharedAddressSpace, defaultCredentials);

        this.managementCredentials = new UserCredentials("artemis-admin", "artemis-admin");
        createOrUpdateUser(sharedAddressSpace, this.managementCredentials);

        amqpClientFactory = new AmqpClientFactory(sharedAddressSpace, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(sharedAddressSpace, defaultCredentials);
    }

    @AfterEach
    public void tearDownShared(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { //test failed
            if (!environment.skipCleanup()) {
                log.info(String.format("test failed: %s.%s",
                        context.getTestClass().get().getName(),
                        context.getTestMethod().get().getName()));
                if (sharedAddressSpace != null) {
                    log.info("shared address space '{}' will be removed", sharedAddressSpace);
                    try {
                        deleteSharedAddressSpace(sharedAddressSpace);
                    } catch (Exception ex) {
                        log.warn("Failed to delete shared address space (ignored)", ex);
                    } finally {
                        spaceCountMap.compute(sharedAddressSpace.getSpec().getType().toLowerCase(), (k, count) -> count == null ? null : count + 1);
                    }
                }
            } else {
                log.warn("Remove address spaces when test failed - SKIPPED!");
            }
        } else { //succeed
            try {
                if (sharedAddressSpace != null) {
                    deleteAddresses(sharedAddressSpace);
                }
            } catch (Exception e) {
                log.warn("Failed to delete addresses from shared address space (ignored)", e);
            }
        }
    }

    @AfterEach
    public void closeAmqpClientFactory() throws Exception {
        if ( amqpClientFactory  != null ) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }
    }

    //================================================================================================
    //====================================== Test help methods =======================================
    //================================================================================================


    /**
     * Create users within groups (according to destNamePrefix and customerIndex), wait until destinations are ready to use
     * and start sending and receiving messages
     *
     * @param dest           list of all available destinations (destinations are not in ready state presumably)
     * @param users          list of users dedicated for sending messages into destinations above
     * @param destNamePrefix prefix of destinations name (due to authorization)
     * @param customerIndex  also important due to authorization (only users under this customer can send messages into dest)
     * @param messageCount   count of messages that will be send into destinations
     * @throws Exception
     */
    protected void doMessaging(List<Address> dest, List<UserCredentials> users, String destNamePrefix, int customerIndex, int messageCount) throws Exception {
        ArrayList<AmqpClient> clients = new ArrayList<>(users.size());
        String sufix = isBrokered(sharedAddressSpace) ? "#" : "*";
        users.forEach((user) -> {
            try {
                createOrUpdateUser(sharedAddressSpace,
                        UserUtils.createUserResource(user)
                                .editSpec()
                                .withAuthorization(Collections.singletonList(
                                        new UserAuthorizationBuilder()
                                                .withAddresses(String.format("%s.%s.%s", destNamePrefix, customerIndex, sufix))
                                                .withOperations(Operation.send, Operation.recv).build()))
                                .endSpec()
                                .done());
                AmqpClient queueClient = amqpClientFactory.createQueueClient();
                queueClient.getConnectOptions().setCredentials(user);
                clients.add(queueClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        waitForDestinationsReady(dest.toArray(new Address[0]));
        //start sending messages
        int everyN = 3;
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<Integer> sent = client.sendMessages(dest.get(i).getSpec().getAddress(), TestUtils.generateMessages(messageCount));
                    //wait for messages sent
                    assertEquals(messageCount, sent.get(1, TimeUnit.MINUTES).intValue(),
                            "Incorrect count of messages send");
                }
            }
        }

        //receive messages
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<List<Message>> received = client.recvMessages(dest.get(i).getSpec().getAddress(), messageCount);
                    //wait for messages received
                    assertEquals(messageCount, received.get(1, TimeUnit.MINUTES).size(),
                            "Incorrect count of messages received");
                }
            }
            client.close();
        }
    }
}
