/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.TlsTermination;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBaseWithMqtt;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.mqtt.MqttPublishTestBase;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.standard.AnycastTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends MqttPublishTestBase implements ITestBaseWithMqtt {

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

    @Test
    void testCustomMessagingRoutes() throws Exception {
        String endpointPrefix = "test-endpoint-";

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()

                .addNewEndpoint()
                .withName(endpointPrefix + "messaging")
                .withService("messaging")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort("amqps")
                .endExpose()
                .endEndpoint()

                .addNewEndpoint()
                .withName(endpointPrefix + "mqtt")
                .withService("mqtt")
                .editOrNewExpose()
                .withType(ExposeType.route)
                .withRouteTlsTermination(TlsTermination.passthrough)
                .withRouteServicePort("secure-mqtt")
                .endExpose()
                .endEndpoint()
                .endSpec()
                .build();
        createAddressSpace(addressSpace);

        UserCredentials luckyUser = new UserCredentials("lucky", "luckyPswd");
        createOrUpdateUser(addressSpace, luckyUser);

        //try to get all external endpoints
        kubernetes.getExternalEndpoint(endpointPrefix + "messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        kubernetes.getExternalEndpoint(endpointPrefix + "mqtt-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));

        //messsaging
        Address anycast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build();
        setAddresses(anycast);
        AmqpClient client1 = amqpClientFactory.createQueueClient(addressSpace);
        client1.getConnectOptions().setCredentials(luckyUser);
        AmqpClient client2 = amqpClientFactory.createQueueClient(addressSpace);
        client2.getConnectOptions().setCredentials(luckyUser);
        AnycastTest.runAnycastTest(anycast, client1, client2);

        //mqtt
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        appendAddresses(topic);
        MqttClientFactory mqttFactory = new MqttClientFactory(addressSpace, luckyUser);
        IMqttClient mqttClient = mqttFactory.create();
        try {
            mqttClient.connect();
            simpleMQTTSendReceive(topic, mqttClient, 3);
            mqttClient.disconnect();
        } finally {
            mqttFactory.close();
        }
    }

    @Override
    protected String topicPlan() {
        return DestinationPlan.STANDARD_LARGE_TOPIC;
    }



}
