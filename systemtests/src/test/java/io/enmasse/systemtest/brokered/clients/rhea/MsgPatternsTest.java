/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.rhea;

import io.enmasse.systemtest.bases.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.MsgPatternsTestBase;
import io.enmasse.systemtest.clients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.clients.rhea.RheaClientSender;
import org.junit.jupiter.api.Test;

public class MsgPatternsTest extends MsgPatternsTestBase implements ITestBaseBrokered {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath), new RheaClientReceiver(logPath), false);
    }

    @Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath),
                new RheaClientReceiver(logPath), new RheaClientReceiver(logPath), false);
    }
}
