/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.standard.web;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.bases.web.WebSocketBrowserTest;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Test;

@SeleniumFirefox
class FirefoxWebSocketBrowserTest extends WebSocketBrowserTest implements ITestSharedStandard {


    @Test
    void testWebSocketSendReceiveQueue() throws Exception {
        doWebSocketSendReceive(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "ws-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("ws-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build());
    }

    @Test
    void testWebSocketSendReceiveTopic() throws Exception {
        doWebSocketSendReceive(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "ws-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("ws-topic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build());
    }
}
