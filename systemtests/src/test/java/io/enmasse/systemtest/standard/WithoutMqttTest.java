/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AuthService;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.ability.ITestBaseWithoutMqtt;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag(isolated)
public class WithoutMqttTest extends TestBase implements ITestBaseWithoutMqtt {
    private AddressSpace addressSpace;

    @BeforeEach
    public void setupSpace() throws Exception {
        addressSpace = new AddressSpace("withoutmqtt", AddressSpaceType.STANDARD, getAddressSpacePlan(),
                AuthService.STANDARD);
        createAddressSpace(addressSpace, false);
        defaultCredentials.setUsername("test");
        defaultCredentials.setPassword("test");
        createUser(addressSpace, defaultCredentials);
        setAddresses(addressSpace, Destination.anycast("a1"));
    }

    @Test
    public void testNoMqttDeployed() throws Exception {
        assertThat(kubernetes.listPods(addressSpace.getNamespace()).size(), is(2));

        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(defaultCredentials);

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult = client.recvMessages("a1", msgs.size());
        Future<Integer> sendResult = client.sendMessages("a1", msgs);

        assertThat("Wrong count of messages sent", sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages received", recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }
}
