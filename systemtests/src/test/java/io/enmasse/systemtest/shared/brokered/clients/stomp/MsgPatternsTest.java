/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.stomp;

import io.enmasse.systemtest.annotations.DefaultMessaging;
import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.messagingclients.stomp.StompClientReceiver;
import io.enmasse.systemtest.messagingclients.stomp.StompClientSender;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;

@Tag(SHARED)
@DefaultMessaging(type = AddressSpaceType.BROKERED, plan = AddressSpacePlans.BROKERED)
class MsgPatternsTest extends ClientTestBase {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new StompClientSender(logPath), new StompClientReceiver(logPath));
    }

    @Test
    @DisplayName("testTopicSubscribe")
    void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest(new StompClientSender(logPath), new StompClientReceiver(logPath), new StompClientReceiver(logPath));
    }

    @Test
    void testStompUserPermissions() throws Exception {
        doTestUserPermissions(new StompClientSender(logPath), new StompClientReceiver(logPath));
    }
}
