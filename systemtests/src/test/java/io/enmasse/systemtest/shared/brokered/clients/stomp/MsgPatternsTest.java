/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.stomp;

import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.stomp.StompClientReceiver;
import io.enmasse.systemtest.messagingclients.stomp.StompClientSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MsgPatternsTest extends ClientTestBase implements ITestSharedBrokered {

    @Override
    protected AbstractClient senderFactory() throws Exception {
        return new StompClientSender(logPath);
    }

    @Override
    protected AbstractClient receiverFactory() throws Exception {
        return new StompClientReceiver(logPath);
    }

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new StompClientSender(logPath), new StompClientReceiver(logPath));
    }

    @Test
    @DisplayName("testTopicSubscribe")
    void testTopicSubscribe() throws Exception {
        doTopicSubscribeTest();
    }

    @Test
    void testStompUserPermissions() throws Exception {
        doTestUserPermissions(new StompClientSender(logPath), new StompClientReceiver(logPath));
    }

}
