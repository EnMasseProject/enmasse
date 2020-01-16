/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.proton.python;

import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.messagingclients.proton.python.PythonClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.python.PythonClientSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MsgPatternsTest extends ClientTestBase implements ITestSharedBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    @DisplayName("testRoundRobinReceiver")
    void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    @DisplayName("testTopicSubscribe")
    void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    void testDrainQueue() throws Exception {
        doDrainQueueTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    @DisplayName("testMessageSelectorTopic")
    void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new PythonClientSender(logPath), new PythonClientSender(logPath),
                new PythonClientReceiver(logPath), new PythonClientReceiver(logPath));
    }
}
