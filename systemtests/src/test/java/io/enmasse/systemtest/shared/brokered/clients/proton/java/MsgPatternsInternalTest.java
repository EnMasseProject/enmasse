/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.clients.proton.java;

import io.enmasse.systemtest.bases.clients.ClusterClientTestBase;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;

@Tag(SHARED)
class MsgPatternsInternalTest extends ClusterClientTestBase {

    @BeforeAll
    void initMessaging() throws Exception {
        resourceManager.createDefaultMessaging(AddressSpaceType.BROKERED, AddressSpacePlans.BROKERED);
    }

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new ProtonJMSClientSender(), new ProtonJMSClientReceiver());
    }
}
