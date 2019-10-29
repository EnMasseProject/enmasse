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

class ConnectionTest extends TestBase implements ITestSharedWithMqtt {
    private static final String CLIENT_ID = "my_client_id";
    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";

    /**
     * MQTT-3.1.4-2 If the ClientId represents a Client already connected to the Server then the Server MUST
     * disconnect the existing Client [].
     */
    @Test
    void newSessionDisconnectsExisting() throws Exception {
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

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);


        IMqttClient client1 = getMqttClientFactory().build().clientId(CLIENT_ID).mqttConnectionOptions(options).create();
        client1.connect();
        // Test we have a usable client.
        publishAndAwaitDelivery(client1);

        CompletableFuture<Void> client1Disconnected = new CompletableFuture<>();
        client1.setCallback((MqttConnectionLostCallback) cause -> client1Disconnected.complete(null));

        IMqttClient client2 = getMqttClientFactory().build().clientId(CLIENT_ID).mqttConnectionOptions(options).create();
        client2.connect();

        get(
                client1Disconnected);
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

    private void get(CompletableFuture<Void> future) throws Exception {
        try {
            future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            TimeoutException timeoutException = new TimeoutException("Client1 should have signalled disconnection");
            timeoutException.initCause(e);
            throw timeoutException;
        }
    }
}
