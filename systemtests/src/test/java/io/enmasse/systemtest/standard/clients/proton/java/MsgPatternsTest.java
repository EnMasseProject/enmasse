/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.clients.proton.java;

import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientSender;
import org.junit.Test;

public class MsgPatternsTest extends io.enmasse.systemtest.standard.clients.MsgPatternsTest {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new ProtonJMSClientSender(), new ProtonJMSClientSender());
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver(), new ProtonJMSClientReceiver());
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver(), new ProtonJMSClientReceiver(), true);
    }

    //@Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver(), new ProtonJMSClientReceiver());
    }

    //@Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver());
    }

    //@Test
    public void testMessageSelectorQueue() throws Exception{
        doMessageSelectorQueueTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver());
    }

    @Test
    public void testMessageSelectorTopic() throws Exception{
        doMessageSelectorTopicTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver(),
                new ProtonJMSClientReceiver(), new ProtonJMSClientReceiver(), true);
    }
}
