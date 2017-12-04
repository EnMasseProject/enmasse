package io.enmasse.systemtest.brokered.clients.artemis;

import io.enmasse.systemtest.executor.client.artemis.ArtemisJMSClientReceiver;
import io.enmasse.systemtest.executor.client.artemis.ArtemisJMSClientSender;
import org.junit.Test;

public class MsgPatternsTest extends io.enmasse.systemtest.brokered.clients.MsgPatternsTest {

    //Disabled until we create mechanism for fetch clients certificates
    //@Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    //@Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    //@Test
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver(), true);
    }

    //@Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    //@Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    //@Test
    public void testMessageSelectorQueue() throws Exception{
        doMessageSelectorQueueTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    //@Test
    public void testMessageSelectorTopic() throws Exception{
        doMessageSelectorTopicTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(),
                new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver(), true);
    }
}
