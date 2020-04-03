/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.openwire;

import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.messagingclients.openwire.OpenwireJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.openwire.OpenwireJMSClientSender;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;

@Tag(SHARED)
class MsgPatternsTest extends ClientTestBase {

    @BeforeAll
    void initMessaging() throws Exception {
        resourceManager.createDefaultMessaging(AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED);
    }

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }

    @Test
    @DisplayName("testRoundRobinReceiver")
    void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }

    @Test
    @DisplayName("testTopicSubscribe")
    void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
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
    @DisplayName("testMessageSelectorTopic")
    void testMessageSelectorTopic() throws Exception {
        doMessageSelectorTopicTest(new OpenwireJMSClientSender(), new OpenwireJMSClientSender(),
                new OpenwireJMSClientReceiver(), new OpenwireJMSClientReceiver());
    }
}
