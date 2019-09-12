/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.proton.python;

import io.enmasse.systemtest.bases.clients.ClusterClientTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedBrokered;
import io.enmasse.systemtest.messagingclients.proton.python.PythonClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.python.PythonClientSender;
import org.junit.jupiter.api.Test;

class MsgPatternsInternalTest extends ClusterClientTestBase implements ITestSharedBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new PythonClientSender(), new PythonClientReceiver());
    }
}
