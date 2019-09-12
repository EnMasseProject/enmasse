/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.shared.standard.mqtt;

import io.enmasse.systemtest.bases.mqtt.MqttPublishTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedWithMqtt;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends MqttPublishTestBase implements ITestSharedWithMqtt {

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

    @Override
    @Test
    @Disabled
    public void testPublishQoS2() throws Exception {
        super.testPublishQoS2();
    }

    @Override
    @Test
    @Disabled("related issue: #1529")
    public void testRetainedMessages() throws Exception {
        super.testRetainedMessages();
    }

    @Override
    protected String topicPlan() {
        return DestinationPlan.STANDARD_LARGE_TOPIC;
    }
}
