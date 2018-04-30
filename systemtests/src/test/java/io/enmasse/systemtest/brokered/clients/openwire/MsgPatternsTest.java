/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.openwire;

import io.enmasse.systemtest.ArtemisManagement;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.clients.openwire.OpenwireJMSClientReceiver;
import io.enmasse.systemtest.clients.openwire.OpenwireJMSClientSender;
import io.enmasse.systemtest.resolvers.ArtemisManagementParameterResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArtemisManagementParameterResolver.class)
class MsgPatternsTest extends ClientTestBase implements ITestBaseBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Test
    void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    @Test
    @Disabled("disabled due to issue #660")
    void testTopicSubscribe(ArtemisManagement artemisManagement) throws Exception {
        doTopicSubscribeTest(artemisManagement, new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver(), true);
    }

    @Test
    void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    @Test
    void testDrainQueue() throws Exception {
        doDrainQueueTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Test
    void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Test
    @Disabled("disabled due to issue #660")
    void testMessageSelectorTopic(ArtemisManagement artemisManagement) throws Exception {
        doMessageSelectorTopicTest(artemisManagement, new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(),
                new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver(), true);
    }
}
