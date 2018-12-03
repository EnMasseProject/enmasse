/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.upgrade;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.upgrade;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag(upgrade)
class UpgradeTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();

    @Test
    void testFunctionalityBeforeAndAfterUpgrade() throws Exception {
        AddressSpace brokered = new AddressSpace("brokered-addr-space", AddressSpaceType.BROKERED, AuthService.STANDARD);
        AddressSpace standard = new AddressSpace("standard-addr-space", AddressSpaceType.STANDARD, AuthService.STANDARD);
        List<Destination> standardAddresses = getAllStandardAddresses();
        List<Destination> brokeredAddresses = getAllBrokeredAddresses();

        List<Destination> brokeredQueues = getQueues(brokeredAddresses);
        List<Destination> standardQueues = getQueues(standardAddresses);

        UserCredentials cred = new UserCredentials("kornelius", "korny");
        int msgCount = 13;

        if (!environment.isUpgraded()) {
            log.info("Before upgrade phase");
            createAddressSpaceList(brokered, standard);

            createUser(brokered, cred);
            createUser(standard, cred);

            setAddresses(brokered, brokeredAddresses.toArray(new Destination[0]));
            setAddresses(standard, standardAddresses.toArray(new Destination[0]));

            assertCanConnect(brokered, cred, brokeredAddresses);
            assertCanConnect(standard, cred, standardAddresses);

            log.info("Send durable messages to brokered queue");
            for (Destination dest : brokeredQueues) {
                sendDurableMessages(brokered, dest, cred, msgCount);
            }
            log.info("Send durable messages to standard queues");
            for (Destination dest : standardQueues) {
                sendDurableMessages(standard, dest, cred, msgCount);
            }
            log.info("End of before upgrade phase");
        } else {
            log.info("After upgrade phase");

            brokered = getAddressSpace(brokered.getName());
            standard = getAddressSpace(standard.getName());

            log.info("Receive durable messages to brokered queue");
            for (Destination dest : brokeredQueues) {
                receiveDurableMessages(brokered, dest, cred, msgCount);
            }
            log.info("Receive durable messages to standard queues");
            for (Destination dest : standardQueues) {
                receiveDurableMessages(standard, dest, cred, msgCount);
            }

            assertCanConnect(brokered, cred, brokeredAddresses);
            assertCanConnect(standard, cred, standardAddresses);

            log.info("End of after upgrade phase");

            log.info("Send durable messages to brokered queue");
            for (Destination dest : brokeredQueues) {
                sendDurableMessages(brokered, dest, cred, msgCount);
            }
            log.info("Send durable messages to standard queues");
            for (Destination dest : standardQueues) {
                sendDurableMessages(standard, dest, cred, msgCount);
            }
        }
    }

    private List<Destination> getQueues(List<Destination> addresses) {
        return addresses.stream().filter(dest -> dest.getType()
                .equals(AddressType.QUEUE.toString())).collect(Collectors.toList());
    }

    private void sendDurableMessages(AddressSpace addressSpace, Destination destination,
                                     UserCredentials credentials, int count) throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);
        List<Message> listOfMessages = new ArrayList<>();
        IntStream.range(0, count).forEach(num -> {
            Message msg = Message.Factory.create();
            msg.setAddress(destination.getAddress());
            msg.setDurable(true);
            listOfMessages.add(msg);
        });
        Future<Integer> sent = client.sendMessages(destination.getAddress(), listOfMessages.toArray(new Message[0]));
        assertThat("Cannot sent durable messages", sent.get(10, TimeUnit.SECONDS), is(count));
        client.close();
    }

    private void receiveDurableMessages(AddressSpace addressSpace, Destination dest,
                                        UserCredentials credentials, int count) throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);
        Future<List<Message>> received = client.recvMessages(dest.getAddress(), count);
        assertThat("Cannot receive durable messages", received.get(10, TimeUnit.SECONDS).size(), is(count));
        client.close();
    }
}
