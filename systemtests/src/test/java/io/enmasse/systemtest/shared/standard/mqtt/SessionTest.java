/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.shared.standard.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedWithMqtt;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.mqtt.MqttDeliveryCompleteCallback;
import io.enmasse.systemtest.mqtt.MqttMessageArrivedCallback;
import io.enmasse.systemtest.utils.AddressUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests related to publish messages via MQTT
 */
public class SessionTest extends TestBase implements ITestSharedWithMqtt {

    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";
    private static final String SUBSCRIBER_ID = "my_subscriber_id";
    private static final String PUBLISHER_ID = "my_publisher_id";


    /**
     * [MQTT-3.1.2-4] If CleanSession is set to 1, the Client and Server MUST discard any previous Session and start a
     * new one.
     */
    @Test
    public void previousPersistentSessionDiscarded() throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), MQTT_TOPIC))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress(MQTT_TOPIC)
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        IMqttClient publisher = getMqttClientFactory().build().clientId(PUBLISHER_ID).create();
        publisher.connect();

        MqttConnectOptions persistentSession = new MqttConnectOptions();
        persistentSession.setCleanSession(false);
        persistentSession.setHttpsHostnameVerificationEnabled(false);

        // Subscribe on a cleanSession false and disconnect
        IMqttClient subscriber = getMqttClientFactory().build().clientId(SUBSCRIBER_ID)
                .mqttConnectionOptions(persistentSession)
                .create();
        subscriber.connect();
        subscriber.subscribe(MQTT_TOPIC, 1);
        subscriber.disconnect();
        subscriber.close();

        publishAndAwaitDelivery(publisher);

        // Connect the subscriber again, with cleanSession false, expect the message to arrive
        CompletableFuture<Void> messageArrived = new CompletableFuture<>();
        subscriber = getMqttClientFactory().build().clientId(SUBSCRIBER_ID)
                .mqttConnectionOptions(persistentSession)
                .create();
        subscriber.setCallback((MqttMessageArrivedCallback) (topic, message) -> messageArrived.complete(null));
        subscriber.connect();
        messageArrived.get();
        subscriber.disconnect();

        // Connect the subscriber again, with cleanSession true, and check that a new message does not arrive
        publishAndAwaitDelivery(publisher);

        MqttConnectOptions cleanSession = new MqttConnectOptions();
        cleanSession.setCleanSession(true);
        cleanSession.setHttpsHostnameVerificationEnabled(false);

        AtomicInteger receivedMessageCount = new AtomicInteger();
        subscriber = getMqttClientFactory().build().clientId(SUBSCRIBER_ID)
                .mqttConnectionOptions(cleanSession)
                .create();
        subscriber.setCallback((MqttMessageArrivedCallback) (topic, message) -> receivedMessageCount.incrementAndGet());
        subscriber.connect();
        // TODO give reasonable time for the message not to arrive
        Thread.sleep(2500);
        subscriber.disconnect();
        assertEquals(0, receivedMessageCount.get(), "Unexpected message was delivered");

        // Connect the subscriber again, with cleanSession true, publish and check the message does not arrive so
        // proving that the subscription was not persisted.
        subscriber = getMqttClientFactory().build().clientId(SUBSCRIBER_ID)
                .mqttConnectionOptions(cleanSession)
                .create();
        subscriber.setCallback((MqttMessageArrivedCallback) (topic, message) -> receivedMessageCount.incrementAndGet());
        subscriber.connect();
        Thread.sleep(2500);
        subscriber.disconnect();
        assertEquals(0, receivedMessageCount.get(), "Unexpected message was delivered");

        publisher.disconnect();
    }

    private void publishAndAwaitDelivery(IMqttClient client) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();

        client.setCallback((MqttDeliveryCompleteCallback) token -> future.complete(null));
        client.publish(MQTT_TOPIC, MQTT_MESSAGE.getBytes(), 1, false);
        future.get();
    }

}
