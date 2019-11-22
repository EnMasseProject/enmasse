/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.soak;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.systemtest.SysytemTestsErrorCollector;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.shared.standard.TopicTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.SOAK;
import static org.hamcrest.CoreMatchers.is;

@Tag(SOAK)
public abstract class SoakTestBase extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AmqpClient> clients = new ArrayList<>();
    private SysytemTestsErrorCollector collector = new SysytemTestsErrorCollector();

    @BeforeEach
    void setupMarathonTests() {
        collector.clear();
    }

    //========================================================================================================
    // Runner tests methods
    //========================================================================================================

    protected void runTestInLoop(int durationMinutes, ThrowableRunner test) throws Exception {
        log.info(String.format("Starting test running for %d minutes at %s",
                durationMinutes, new Date().toString()));
        int fails = 0;
        int limit = 10;
        int i = 0;
        for (long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(durationMinutes); stop > System.nanoTime(); ) {
            try {
                log.info("*********************************** Test run {} ***********************************", ++i);
                test.run();
                fails = 0;
            } catch (Exception ex) {
                log.warn("Test run {} failed with: {}", i, ex.getMessage());
                collector.addError(ex);
                if (++fails >= limit) {
                    throw new IllegalStateException(String.format("Test failed: %d times in a row: %s", fails, collector.toString()));
                }
            } finally {
                closeClients();
                log.info("***********************************************************************************");
                Thread.sleep(60_000);
            }
        }
        if (!collector.verify()) {
            throw new IllegalStateException(String.format("Test failed with these exceptions: %s", collector.toString()));
        }
    }

    private void closeClients() {
        for (AmqpClient client : clients) {
            try {
                client.close();
                log.info("Client is closed.");
            } catch (Exception ex) {
                collector.addError(ex);
            }
        }
        clients.clear();
    }

    //========================================================================================================
    // Tests methods
    //========================================================================================================
    protected void doTestQueueSendReceiveLong(AddressSpace addressSpace) throws Exception {
        resourcesManager.createAddressSpace(addressSpace);

        int msgCount = 1000;
        int queueCount = 10;
        int senderCount = 10;
        int recvCount = 20;

        List<Address> queueList = new ArrayList<>();

        //create queues
        for (int i = 0; i < queueCount; i++) {
            queueList.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-sendreceive-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("queue-sendreceive-" + i)
                    .withPlan(getDefaultPlan(AddressType.QUEUE))
                    .endSpec()
                    .build());
        }
        resourcesManager.setAddresses(queueList.toArray(new Address[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        UserCredentials credentials = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpace, credentials);

        runTestInLoop(30, () -> {
            //create client
            AmqpClient client = resourcesManager.getAmqpClientFactory().createQueueClient(addressSpace);
            client.getConnectOptions().setCredentials(credentials);
            clients.add(client);

            //attach receivers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < recvCount / 2; i++) {
                recvResults.add(client.recvMessages(queueList.get(i).getSpec().getAddress(), msgCount / 2));
                recvResults.add(client.recvMessages(queueList.get(i).getSpec().getAddress(), msgCount / 2));
            }

            //attach senders
            for (int i = 0; i < senderCount; i++) {
                collector.checkThat(client.sendMessages(queueList.get(i).getSpec().getAddress(), msgBatch).get(2, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < recvCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount / 2));
            }
        });
    }

    protected void doTestAuthSendReceiveLong(AddressSpace addressSpace) throws Exception {
        log.info("testAuthSendReceiveLong start");
        resourcesManager.createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-auth-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-auth-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-auth-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-auth-topic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue, topic);
        log.info("Addresses '{}', '{}' created", queue.getSpec().getAddress(), topic.getSpec().getAddress());

        UserCredentials user = new UserCredentials("test-user", "test-user");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        runTestInLoop(30, () -> {
            log.info("Start test loop basic auth tests");
            getClientUtils().assertCanConnect(addressSpace, user, Arrays.asList(queue, topic), resourcesManager);
            getClientUtils().assertCannotConnect(addressSpace, new UserCredentials("nobody", "nobody"), Arrays.asList(queue, topic), resourcesManager);
        });
        log.info("testAuthSendReceiveLong finished");
    }

    protected void doTestTopicPubSubLong(AddressSpace addressSpace) throws Exception {
        resourcesManager.createAddressSpace(addressSpace);

        int msgCount = 1000;
        int topicCount = 10;

        List<Address> topicList = new ArrayList<>();

        //create queues
        for (int i = 0; i < topicCount; i++) {
            topicList.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic-pubsub-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("topic")
                    .withAddress("test-topic-pubsub-" + i)
                    .withPlan(getDefaultPlan(AddressType.TOPIC))
                    .endSpec()
                    .build());
        }
        resourcesManager.setAddresses(topicList.toArray(new Address[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        UserCredentials credentials = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpace, credentials);
        runTestInLoop(30, () -> {
            AmqpClient client = resourcesManager.getAmqpClientFactory().createTopicClient(addressSpace);
            client.getConnectOptions().setCredentials(credentials);
            clients.add(client);

            //attach subscibers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < topicCount; i++) {
                recvResults.add(client.recvMessages(String.format("test-topic-pubsub-%d", i), msgCount));
            }

            //attach producers
            for (int i = 0; i < topicCount; i++) {
                collector.checkThat(client.sendMessages(topicList.get(i).getSpec().getAddress(), msgBatch).get(2, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < topicCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount));
            }
        });
    }

    //========================================================================================================
    // Help methods
    //========================================================================================================

    private void doAddressTest(AddressSpace addressSpace, String topicPattern,
                               String queuePattern, UserCredentials credentials) throws Exception {
        List<Address> queueList = new ArrayList<>();
        List<Address> topicList = new ArrayList<>();

        int destinationCount = 20;

        for (int i = 0; i < destinationCount; i++) {
            queueList.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, queuePattern + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("queue-via-web")
                    .withPlan(getDefaultPlan(AddressType.QUEUE))
                    .endSpec()
                    .build());
            topicList.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, topicPattern + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("topic")
                    .withAddress(topicPattern + i)
                    .withPlan(getDefaultPlan(AddressType.TOPIC))
                    .endSpec()
                    .build());
        }

        AmqpClient queueClient;
        AmqpClient topicClient;

        resourcesManager.setAddresses(queueList.toArray(new Address[0]));
        resourcesManager.appendAddresses(topicList.toArray(new Address[0]));

        queueClient = resourcesManager.getAmqpClientFactory().createQueueClient(addressSpace);
        queueClient.getConnectOptions().setCredentials(credentials);
        clients.add(queueClient);

        topicClient = resourcesManager.getAmqpClientFactory().createTopicClient(addressSpace);
        topicClient.getConnectOptions().setCredentials(credentials);
        clients.add(topicClient);

        for (Address queue : queueList) {
            QueueTest.runQueueTest(queueClient, queue, 1024);
        }

        for (Address topic : topicList) {
            TopicTest.runTopicTest(topicClient, topic, 1024);
        }

        resourcesManager.deleteAddresses(queueList.toArray(new Address[0]));
        resourcesManager.deleteAddresses(topicList.toArray(new Address[0]));
        Thread.sleep(15000);
    }

    protected void doTestLoad(AddressSpaceType type, String addressSpacePlans, String addressPlan) throws Exception {

        runTestInLoop(120, () -> {
            int count = 5;
            AmqpClient queueClient;
            Map<Address, AmqpClient> pairs = new HashMap<Address, AmqpClient>();


            List<AddressSpace> addressSpaceList = IntStream.range(0, count).mapToObj(i ->
                    new AddressSpaceBuilder()
                            .withNewMetadata()
                            .withName("test-address-space-" + i)
                            .withNamespace(kubernetes.getInfraNamespace())
                            .endMetadata()
                            .withNewSpec()
                            .withType(type.toString())
                            .withPlan(addressSpacePlans)
                            .withNewAuthenticationService()
                            .withName("standard-authservice")
                            .endAuthenticationService()
                            .endSpec()
                            .build()).collect(Collectors.toList());

            resourcesManager.createAddressSpace(addressSpaceList.toArray(new AddressSpace[0]));
            List<Address> addresses = new LinkedList<>();
            for (AddressSpace space : addressSpaceList) {
                UserCredentials credentials = new UserCredentials("test", "test");
                resourcesManager.createOrUpdateUser(space, credentials);
                queueClient = resourcesManager.getAmqpClientFactory().createQueueClient(space);
                queueClient.getConnectOptions().setCredentials(credentials);
                clients.add(queueClient);

                for (int i = 0; i < count; i++) {
                    addresses.add(new AddressBuilder()
                            .withNewMetadata()
                            .withNamespace(space.getMetadata().getNamespace())
                            .withName(AddressUtils.generateAddressMetadataName(space, "test-address-" + i))
                            .endMetadata()
                            .withNewSpec()
                            .withType("queue")
                            .withAddress("test-address-" + i)
                            .withPlan(addressPlan)
                            .endSpec()
                            .build());
                    pairs.put(addresses.get(i), queueClient);
                }
            }
            resourcesManager.setAddresses(addresses.toArray(new Address[0]));
            for (Map.Entry<Address, AmqpClient> pair : pairs.entrySet()) {
                QueueTest.runQueueTest(pair.getValue(), pair.getKey(), 1024);
            }

            for (AddressSpace space : addressSpaceList) {
                resourcesManager.deleteAddressSpace(space);
            }
        });
    }
}







