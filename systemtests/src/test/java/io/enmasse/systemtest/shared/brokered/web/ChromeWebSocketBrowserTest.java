/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.annotations.DefaultMessaging;
import io.enmasse.systemtest.annotations.SeleniumChrome;
import io.enmasse.systemtest.bases.web.WebSocketBrowserTest;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.SHARED;

@Tag(NON_PR)
@Tag(SHARED)
@DefaultMessaging(type = AddressSpaceType.BROKERED, plan = AddressSpacePlans.BROKERED)
@SeleniumChrome
class ChromeWebSocketBrowserTest extends WebSocketBrowserTest {
    
    @Test
    void testWebSocketSendReceiveQueue() throws Exception {
        doWebSocketSendReceive(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "ws-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("ws-queue")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }

    @Test
    void testWebSocketSendReceiveTopic() throws Exception {
        doWebSocketSendReceive(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(resourceManager.getDefaultAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(resourceManager.getDefaultAddressSpace(), "ws-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("ws-topic")
                .withPlan(resourceManager.getDefaultAddressPlan(AddressType.TOPIC))
                .endSpec()
                .build());
    }
}
