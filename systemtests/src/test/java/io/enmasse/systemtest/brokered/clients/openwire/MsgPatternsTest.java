/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.openwire;

import io.enmasse.systemtest.bases.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.MsgPatternsTestBase;
import io.enmasse.systemtest.clients.openwire.OpenwireJMSClientReceiver;
import io.enmasse.systemtest.clients.openwire.OpenwireJMSClientSender;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MsgPatternsTest extends MsgPatternsTestBase implements ITestBaseBrokered {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    @Test
    @Disabled("disabled due to issue #660")
    public void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver(), true);
    }

    @Test
    public void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    @Test
    public void testDrainQueue() throws Exception {
        doDrainQueueTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Test
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Disabled("disabled due to issue #660")
    public void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(),
                new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver(), true);
    }
}
