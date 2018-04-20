/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.clients.proton.java;

import io.enmasse.systemtest.ArtemisManagement;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.resolvers.ArtemisManagementParameterResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArtemisManagementParameterResolver.class)
public class MsgPatternsTest extends ClientTestBase implements ITestBaseStandard {

    @Test
    public void testBasicMessage() throws Exception {
        doBasicMessageTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testRoundRobinReceiver() throws Exception {
        doRoundRobinReceiverTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testTopicSubscribe(ArtemisManagement artemisManagement) throws Exception {
        doTopicSubscribeTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }

    @Test
    @Disabled("selectors for queue does not work")
    public void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    public void testMessageSelectorTopic(ArtemisManagement artemisManagement) throws Exception {
        doMessageSelectorTopicTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath),
                new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }
}
