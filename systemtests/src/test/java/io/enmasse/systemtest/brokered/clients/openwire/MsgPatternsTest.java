package io.enmasse.systemtest.brokered.clients.openwire;

import io.enmasse.systemtest.executor.client.openwire.OpenwireJMSClientReceiver;
import io.enmasse.systemtest.executor.client.openwire.OpenwireJMSClientSender;

public class MsgPatternsTest extends io.enmasse.systemtest.brokered.clients.MsgPatternsTest {

    //Disabled until we create mechanism for fetch clients certificates
    //@Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    //@Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    //@Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver(), true);
    }

    //@Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    //@Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    //@Test
    public void testMessageSelectorQueue() throws Exception{
        doMessageSelectorQueueTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    //@Test
    public void testMessageSelectorTopic() throws Exception{
        doMessageSelectorTopicTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(),
                new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver(), true);
    }
}
