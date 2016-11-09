/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.smoketest;

import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TopicTest extends VertxTestBase{

    @Test
    public void testMultipleSubscribers() throws Exception {
        Destination dest = Destination.topic("mytopic");
        deploy(dest);
        scale(dest, 4);
        EnMasseClient client = createTopicClient();
        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        List<Future<List<String>>> recvResults = Arrays.asList(
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()));

        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    public void testSubscriptionService() throws Exception {
        Destination dest = Destination.topic("mytopic");
        deploy(dest);
        String address = "myaddress";

        EnMasseClient ctrlClient = createQueueClient();
        EnMasseClient client = createTopicClient();

        System.out.println("Waiting for the system to stabilize");
        Thread.sleep(300_000);

        System.out.println("Creating subscription");
        Message sub = Message.Factory.create();
        sub.setAddress("$subctrl");
        sub.setCorrelationId(address);
        sub.setSubject("subscribe");
        sub.setApplicationProperties(new ApplicationProperties(Collections.singletonMap("root_address", dest.getAddress())));
        ctrlClient.sendMessages("$subctrl", sub).get(5, TimeUnit.MINUTES);

        Thread.sleep(20000);
        System.out.println("Starting to send messages");

        List<String> msgs = Arrays.asList("foo", "bar", "baz");
        Future<List<String>> recvResult = client.recvMessages(address, msgs.size());

        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));

        Message unsub = Message.Factory.create();
        unsub.setAddress("$subctrl");
        unsub.setCorrelationId(address);
        unsub.setApplicationProperties(new ApplicationProperties(Collections.singletonMap("root_address", dest.getAddress())));
        unsub.setSubject("unsubscribe");
        ctrlClient.sendMessages("$subctrl", unsub).get(5, TimeUnit.MINUTES);
    }

    public void testScaledown() throws Exception {
        Destination dest = Destination.topic("mytopic");
        deploy(dest);
        scale(dest, 2);

        EnMasseClient client = createDurableTopicClient();
        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        List<Future<List<String>>> recvResults = Arrays.asList(
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()));

        assertThat(client.sendMessages(dest.getAddress(), msgs.subList(0, 2)).get(1, TimeUnit.MINUTES), is(2));

        Thread.sleep(5000);

        scale(dest, 1);

        Thread.sleep(5000);

        assertThat(client.sendMessages(dest.getAddress(), msgs.subList(2, 3)).get(1, TimeUnit.MINUTES), is(1));

        Thread.sleep(5000);
        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }
}
