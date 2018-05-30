/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.artemis;

import io.enmasse.systemtest.ArtemisManagement;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.clients.artemis.ArtemisJMSClientReceiver;
import io.enmasse.systemtest.clients.artemis.ArtemisJMSClientSender;
import io.enmasse.systemtest.resolvers.ArtemisManagementParameterResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArtemisManagementParameterResolver.class)
class MsgPatternsTest extends ClientTestBase implements ITestBaseBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver());
    }

    @Test
    void testRoundRobinReceiver(ArtemisManagement artemisManagement) throws Exception {
        doRoundRobinReceiverTest(artemisManagement, new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver());
    }

    @Test
    void testTopicSubscribe(ArtemisManagement artemisManagement) throws Exception {
        doTopicSubscribeTest(artemisManagement, new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver(), true);
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
    void testMessageSelectorTopic(ArtemisManagement artemisManagement) throws Exception {
        doMessageSelectorTopicTest(artemisManagement, new ArtemisJMSClientSender(), new ArtemisJMSClientReceiver(),
                new ArtemisJMSClientReceiver(), new ArtemisJMSClientReceiver(), true);
    }
}
