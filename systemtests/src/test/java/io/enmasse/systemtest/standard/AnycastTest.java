/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.amqp.AmqpClient;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AnycastTest extends StandardTestBase {

    @Test
    public void testMultipleReceivers() throws Exception {
        Destination dest = Destination.anycast("anycastMultipleReceivers");
        setAddresses(dest);
        AmqpClient client1 = amqpClientFactory.createQueueClient();
        AmqpClient client2 = amqpClientFactory.createQueueClient();
        AmqpClient client3 = amqpClientFactory.createQueueClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult1 = client1.recvMessages(dest.getAddress(), 1);
        Future<List<Message>> recvResult2 = client2.recvMessages(dest.getAddress(), 1);
        Future<List<Message>> recvResult3 = client3.recvMessages(dest.getAddress(), 1);
        Future<Integer> sendResult = client1.sendMessages(dest.getAddress(), msgs);

        assertThat("Wrong count of messages sent", sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat("Wrong count of messages received: receiver1",
                recvResult1.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat("Wrong count of messages received: receiver2",
                recvResult2.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat("Wrong count of messages received: receiver3",
                recvResult3.get(1, TimeUnit.MINUTES).size(), is(1));
    }

    @Test
    public void testRestApi() throws Exception {
        List<String> addresses = Arrays.asList("anycastRest1", "anycastRest2");
        Destination a1 = Destination.anycast(addresses.get(0));
        Destination a2 = Destination.anycast(addresses.get(1));

        runRestApiTest(addresses, a1, a2);
    }

}
