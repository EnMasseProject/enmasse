/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.shared.brokered.mqtt;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.annotations.DefaultMessaging;
import io.enmasse.systemtest.bases.mqtt.MqttPublishTestBase;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory.Builder;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.enmasse.systemtest.TestTag.SHARED;

@Tag(SHARED)
@DefaultMessaging(type = AddressSpaceType.BROKERED, plan = AddressSpacePlans.BROKERED)
public class PublishTest extends MqttPublishTestBase {

    @Test
    @Override
    public void testPublishQoS0() throws Exception {
        super.testPublishQoS0();
    }

    @Test
    @Override
    public void testPublishQoS1() throws Exception {
        super.testPublishQoS1();
    }

    @Test
    @Override
    public void testPublishQoS2() throws Exception {
        super.testPublishQoS2();
    }

    @Test
    @Override
    public void testRetainedMessages() throws Exception {
        super.testRetainedMessages();
    }

    @Override
    protected void customizeClient(Builder mqttClientBuilder) {
        Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByServiceName(resourceManager.getDefaultAddressSpace(), "messaging");
        if (messagingEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(resourceManager.getDefaultAddressSpace(), "messaging");
            messagingEndpoint = Kubernetes.getInstance().getExternalEndpoint(externalEndpointName + "-" + AddressSpaceUtils.getAddressSpaceInfraUuid(resourceManager.getDefaultAddressSpace()));
        }
        mqttClientBuilder.endpoint(messagingEndpoint);
    }


}
