/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category(IsolatedAddressSpace.class)
public class WithoutMqttTest extends TestBase {
    @Override
    protected String getDefaultPlan(AddressType addressType) {
        return "standard-anycast";
    }

    @Test
    public void testNoMqttDeployed() throws Exception {
        AddressSpace addressSpace = new AddressSpace("withoutmqtt", AddressSpaceType.STANDARD, "unlimited-standard-without-mqtt");
        try {
            createAddressSpace(addressSpace, "standard");
            setAddresses(addressSpace, Destination.anycast("a1"));

            assertThat(kubernetes.listPods(addressSpace.getNamespace()).size(), is(2));

            AmqpClient client = amqpClientFactory.createQueueClient();

            List<String> msgs = Arrays.asList("foo", "bar", "baz");

            Future<List<Message>> recvResult = client.recvMessages("a1", msgs.size());
            Future<Integer> sendResult = client.sendMessages("a1", msgs);

            assertThat("Wrong count of messages sent", sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));
            assertThat("Wrong count of messages received", recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        } finally {
            deleteAddressSpace(addressSpace);
        }
    }
}
