/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.shared.standard.mqtt;

import io.enmasse.systemtest.bases.mqtt.MqttPublishTestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedWithMqtt;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.github.artsok.RepeatedIfExceptionsTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends MqttPublishTestBase implements ITestSharedWithMqtt {

    @DisplayName("testPublishQoS0")
    @RepeatedIfExceptionsTest(repeats = 2, name = "")
    @Override
    public void testPublishQoS0() throws Exception {
        super.testPublishQoS0();
    }

    @DisplayName("testPublishQoS1")
    @RepeatedIfExceptionsTest(repeats = 2, name = "")
    @Override
    public void testPublishQoS1() throws Exception {
        super.testPublishQoS1();
    }

    @Override
    @DisplayName("testPublishQoS2")
    @RepeatedIfExceptionsTest(repeats = 2, name = "")
    @Disabled
    public void testPublishQoS2() throws Exception {
        super.testPublishQoS2();
    }

    @Override
    @DisplayName("testRetainedMessages")
    @RepeatedIfExceptionsTest(repeats = 2, name = "")
    @Disabled("related issue: #1529")
    public void testRetainedMessages() throws Exception {
        super.testRetainedMessages();
    }

    @Override
    protected String topicPlan() {
        return DestinationPlan.STANDARD_LARGE_TOPIC;
    }
}
