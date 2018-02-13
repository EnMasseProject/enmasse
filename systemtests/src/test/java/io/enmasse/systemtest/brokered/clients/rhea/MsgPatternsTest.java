/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.rhea;

import io.enmasse.systemtest.clients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.clients.rhea.RheaClientSender;
import org.junit.Test;

public class MsgPatternsTest extends io.enmasse.systemtest.brokered.clients.MsgPatternsTest {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new RheaClientSender(), new RheaClientReceiver());
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new RheaClientSender(), new RheaClientReceiver(), new RheaClientReceiver());
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new RheaClientSender(), new RheaClientReceiver(), new RheaClientReceiver(), false);
    }

    @Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new RheaClientSender(), new RheaClientReceiver(), new RheaClientReceiver());
    }

    @Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new RheaClientSender(), new RheaClientReceiver());
    }

    @Test
    public void testMessageSelectorQueue() throws Exception{
        doMessageSelectorQueueTest(new RheaClientSender(), new RheaClientReceiver());
    }

    @Test
    public void testMessageSelectorTopic() throws Exception{
        doMessageSelectorTopicTest(new RheaClientSender(), new RheaClientReceiver(),
                new RheaClientReceiver(), new RheaClientReceiver(), false);
    }
}
