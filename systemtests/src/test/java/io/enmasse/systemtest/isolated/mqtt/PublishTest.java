/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.TlsTermination;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.isolated.standard.AnycastTest;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.MessagingUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.junit.jupiter.api.Test;

public class PublishTest extends TestBase implements ITestIsolatedStandard {

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
        resourcesManager.createAddressSpace(addressSpace);

        UserCredentials luckyUser = new UserCredentials("lucky", "luckyPswd");
        resourcesManager.createOrUpdateUser(addressSpace, luckyUser);

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
        resourcesManager.setAddresses(anycast);
        AmqpClient client1 = getAmqpClientFactory().createQueueClient(addressSpace);
        client1.getConnectOptions().setCredentials(luckyUser);
        AmqpClient client2 = getAmqpClientFactory().createQueueClient(addressSpace);
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
        resourcesManager.appendAddresses(topic);
        MqttClientFactory mqttFactory = new MqttClientFactory(addressSpace, luckyUser);
        IMqttClient mqttClient = mqttFactory.create();
        try {
            mqttClient.connect();
            MessagingUtils.simpleMQTTSendReceive(topic, mqttClient, 3);
            mqttClient.disconnect();
        } finally {
            mqttFactory.close();
        }
    }
}
