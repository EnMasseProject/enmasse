/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.clients.proton.python;

import io.enmasse.systemtest.bases.ITestBaseStandard;
import io.enmasse.systemtest.bases.clients.MsgPatternsTestBase;
import io.enmasse.systemtest.clients.proton.python.PythonClientReceiver;
import io.enmasse.systemtest.clients.proton.python.PythonClientSender;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MsgPatternsTest extends MsgPatternsTestBase implements ITestBaseStandard {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath), new PythonClientReceiver(logPath), false);
    }

    @Test
    @Disabled("selectors for queue does not work")
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new PythonClientSender(logPath), new PythonClientReceiver(logPath),
                new PythonClientReceiver(logPath), new PythonClientReceiver(logPath), false);
    }
}
