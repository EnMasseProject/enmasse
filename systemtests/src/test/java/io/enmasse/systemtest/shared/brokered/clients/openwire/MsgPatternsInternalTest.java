/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.openwire;

import io.enmasse.systemtest.bases.clients.ClusterClientTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.messagingclients.openwire.OpenwireJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.openwire.OpenwireJMSClientSender;
import org.junit.jupiter.api.Test;

class MsgPatternsInternalTest extends ClusterClientTestBase implements ITestSharedBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new OpenwireJMSClientSender(), new OpenwireJMSClientReceiver());
    }
}

