/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.artemis;

import io.enmasse.systemtest.bases.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.MsgPatternsTestBase;
import io.enmasse.systemtest.clients.artemis.ArtemisJMSClientReceiver;
import io.enmasse.systemtest.clients.artemis.ArtemisJMSClientSender;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("core client has still issue with trustAll")
public class MsgPatternsTest extends MsgPatternsTestBase implements ITestBaseBrokered {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver(), true);
    }

    @Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    @Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    public void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(),
                new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver(), true);
    }
}
