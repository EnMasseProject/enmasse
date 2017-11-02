package io.enmasse.systemtest.clients.rhea;

import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;
import org.junit.Test;

public class BasicClientTest extends io.enmasse.systemtest.clients.BasicClientTest {

    @Test
    public void basicMessageTest() throws Exception {
        doBasicMessageTest(new RheaClientSender(), new RheaClientReceiver());
    }

    @Test
    public void roundRobinReceiverTest() throws Exception {
        doRoundRobinReceiverTest(new RheaClientSender(), new RheaClientReceiver(), new RheaClientReceiver());
    }

    @Test
    public void topicSubscribeTest() throws Exception {
        doTopicSubscribeTest(new RheaClientSender(), new RheaClientReceiver());
    }

    @Test
    public void messageBrowseTest() throws Exception {
        doMessageBrowseTest(new RheaClientSender(), new RheaClientReceiver(), new RheaClientReceiver());
    }

    @Test
    public void drainQueueTest() throws Exception {
        doDrainQueueTest(new RheaClientSender(), new RheaClientReceiver());
    }

    @Test
    public void messageSelectorTest() throws Exception{
        doMessageSelectorTest(new RheaClientSender(), new RheaClientReceiver());
    }
}
