/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.ability.ITestBaseWithMqtt;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.mqtt.MqttConnectionLostCallback;
import io.enmasse.systemtest.mqtt.MqttDeliveryCompleteCallback;
import io.enmasse.systemtest.utils.AddressUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConnectionTest extends TestBaseWithShared implements ITestBaseWithMqtt {
    private static final String CLIENT_ID = "my_client_id";
    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";

    /**
     * MQTT-3.1.4-2 If the ClientId represents a Client already connected to the Server then the Server MUST
     * disconnect the existing Client [].
     */
    @Test
    public void newSessionDisconnectsExisting() throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(sharedAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(sharedAddressSpace, MQTT_TOPIC))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress(MQTT_TOPIC)
                .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                .endSpec()
                .build();
        setAddresses(dest);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);


        IMqttClient client1 = mqttClientFactory.build().clientId(CLIENT_ID).mqttConnectionOptions(options).create();
        client1.connect();
        // Test we have a usable client.
        publishAndAwaitDelivery(client1);

        CompletableFuture<Void> client1Disconnected = new CompletableFuture<>();
        client1.setCallback((MqttConnectionLostCallback) cause -> client1Disconnected.complete(null));

        IMqttClient client2 = mqttClientFactory.build().clientId(CLIENT_ID).mqttConnectionOptions(options).create();
        client2.connect();

        get("Client1 should have signalled disconnection",
                30, TimeUnit.SECONDS, client1Disconnected);
        assertFalse(client1.isConnected(), "Client1 should have been disconnected.");
        client1.close();

        // Test that client2 remains usable
        publishAndAwaitDelivery(client2);

        client2.disconnect();
        client2.close();
    }

    private void publishAndAwaitDelivery(IMqttClient client) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();

        client.setCallback((MqttDeliveryCompleteCallback) token -> future.complete(null));
        client.publish(MQTT_TOPIC, MQTT_MESSAGE.getBytes(), 1, false);
        future.get();
    }

    private void get(String message, int timeout, TimeUnit minutes, CompletableFuture<Void> future) throws Exception {
        try {
            future.get(timeout, minutes);
        } catch (TimeoutException e) {
            TimeoutException timeoutException = new TimeoutException(message);
            timeoutException.initCause(e);
            throw timeoutException;
        }
    }
}
