/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.clients.rhea;

import io.enmasse.systemtest.ArtemisManagement;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.clients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.clients.rhea.RheaClientSender;
import io.enmasse.systemtest.resolvers.ArtemisManagementParameterResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArtemisManagementParameterResolver.class)
public class MsgPatternsTest extends ClientTestBase implements ITestBaseStandard {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    @Disabled("tests is disabled until websocket will be enabled in standard addr space")
    public void testBasicMessageWebScoket() throws Exception {
        doBasicMessageTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath), true);
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testTopicSubscribe(ArtemisManagement artemisManagement) throws Exception {
        doTopicSubscribeTest(artemisManagement, new RheaClientSender(logPath), new RheaClientReceiver(logPath), new RheaClientReceiver(logPath), false);
    }

    @Test
    @Disabled("selectors for queue does not work")
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new RheaClientSender(logPath), new RheaClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorTopic(ArtemisManagement artemisManagement) throws Exception {
        doMessageSelectorTopicTest(artemisManagement, new RheaClientSender(logPath), new RheaClientReceiver(logPath),
                new RheaClientReceiver(logPath), new RheaClientReceiver(logPath), false);
    }
}
