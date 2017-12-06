package io.enmasse.systemtest.brokered.clients.proton.python;

import io.enmasse.systemtest.executor.client.proton.python.PythonClientReceiver;
import io.enmasse.systemtest.executor.client.proton.python.PythonClientSender;

import org.junit.Test;

public class MsgPatternsTest extends io.enmasse.systemtest.brokered.clients.MsgPatternsTest {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new PythonClientSender(), new PythonClientReceiver());
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new PythonClientSender(), new PythonClientReceiver(), new PythonClientReceiver());
    }

    @Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new PythonClientSender(), new PythonClientReceiver(), new PythonClientReceiver(), false);
    }

    @Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new PythonClientSender(), new PythonClientReceiver(), new PythonClientReceiver());
    }

    @Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new PythonClientSender(), new PythonClientReceiver());
    }

    @Test
    public void testMessageSelectorQueue() throws Exception{
        doMessageSelectorQueueTest(new PythonClientSender(), new PythonClientReceiver());
    }

    @Test
    public void testMessageSelectorTopic() throws Exception{
        doMessageSelectorTopicTest(new PythonClientSender(), new PythonClientReceiver(),
                new PythonClientReceiver(), new PythonClientReceiver(), false);
    }
}
