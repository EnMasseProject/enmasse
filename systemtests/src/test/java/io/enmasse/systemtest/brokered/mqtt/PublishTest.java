/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.mqtt;

import org.junit.jupiter.api.Test;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.mqtt.MqttPublishTestBase;
import io.enmasse.systemtest.mqtt.MqttClientFactory.Builder;
import io.enmasse.systemtest.utils.AddressSpaceUtils;

public class PublishTest extends MqttPublishTestBase implements ITestBaseBrokered{

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
        Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByServiceName(sharedAddressSpace, "messaging");
        if (messagingEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(sharedAddressSpace, "messaging");
            messagingEndpoint = Kubernetes.getInstance().getExternalEndpoint(externalEndpointName + "-" + AddressSpaceUtils.getAddressSpaceInfraUuid(sharedAddressSpace));
        }
        mqttClientBuilder.endpoint(messagingEndpoint);
    }



}
