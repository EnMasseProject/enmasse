/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.proton.java;

import io.enmasse.systemtest.bases.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.MsgPatternsTestBase;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientSender;
import org.junit.jupiter.api.Test;

public class MsgPatternsTest extends MsgPatternsTestBase implements ITestBaseBrokered {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }

    @Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath),
                new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }
}
