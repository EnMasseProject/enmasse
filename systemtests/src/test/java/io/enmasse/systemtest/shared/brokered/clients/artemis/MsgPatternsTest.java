/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.artemis;

import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.messagingclients.artemis.ArtemisJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.artemis.ArtemisJMSClientSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MsgPatternsTest extends ClientTestBase implements ITestSharedBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    @DisplayName("testRoundRobinReceiver")
    void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    @Test
    @DisplayName("testTopicSubscribe")
    void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    @Test
    void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    @Test
    void testDrainQueue() throws Exception {
        doDrainQueueTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    @DisplayName("testMessageSelectorTopic")
    void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new ArtemisJMSClientSender(), new ArtemisJMSClientSender(),
                new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }
}
