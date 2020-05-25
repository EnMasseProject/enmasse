/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.ThrowingConsumer;
import io.vertx.core.buffer.Buffer;

public class MqttAdapterClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttAdapterClient.class);
    private IMqttAsyncClient adapterClient;

    private MqttAdapterClient(final IMqttAsyncClient adapterClient) {
        this.adapterClient = adapterClient;
    }

    @Override
    public void close() throws Exception {
        this.adapterClient.close();
    }

    public boolean sendQos0(MessageSendTester.Type type, Buffer json, Duration timeout) {
        return send(0, type, json, timeout);
    }

    public boolean sendQos1(MessageSendTester.Type type, Buffer json, Duration timeout) {
        return send(1, type, json, timeout);
    }

    public boolean send(int qos, final MessageSendTester.Type type, final Buffer json, final Duration timeout) {
        final MqttMessage message = new MqttMessage(json.getBytes());
        message.setQos(qos);
        try {
            final IMqttDeliveryToken token = this.adapterClient.publish(type.type().address(), message);
            if (qos <= 0) {
                return true; // we know nothing with QoS 0
            }
            token.waitForCompletion(timeout.toMillis());
        } catch (Exception e) {
            log.info("Failed to send MQTT message", e);
            return false;
        }
        return true;
    }

    public static MqttAdapterClient create(final Endpoint endpoint, final String deviceId, final PrivateKey key, final X509Certificate certificate )
            throws Exception {

        return create(endpoint, deviceId, builder -> {
            builder.clientCertificate(KeyStoreCreator.from(key,certificate));
        });

    }

    public static MqttAdapterClient create(final Endpoint endpoint, final String deviceId, final String deviceAuthId, final String tenantId, final String devicePassword)
            throws Exception {

        return create(endpoint, deviceId, builder -> {
            builder.usernameAndPassword(deviceAuthId + "@" + tenantId, devicePassword);
        });

    }

    private static MqttAdapterClient create(final Endpoint endpoint, final String deviceId, final ThrowingConsumer<MqttClientFactory.Builder> builder)
            throws Exception {

        var adapterClientBuilder = new MqttClientFactory.Builder()
                .clientId(deviceId)
                .endpoint(endpoint)
                .mqttConnectionOptions(options -> {
                    options.setAutomaticReconnect(true);
                    options.setConnectionTimeout(60);
                    // do not reject due to "inflight" messages. Note: this will allocate an array of that size.
                    options.setMaxInflight(16 * 1024);
                    options.setHttpsHostnameVerificationEnabled(false);
                });

        builder.accept(adapterClientBuilder);

        var adapterClient = adapterClientBuilder.createAsync();

        try {
            TestUtils.waitUntilCondition("Successfully connect to mqtt adapter", phase -> {
                try {
                    adapterClient.connect().waitForCompletion(10_000);
                    return true;
                } catch (MqttException mqttException) {
                    if (phase == WaitPhase.LAST_TRY) {
                        log.error("Error waiting to connect mqtt adapter", mqttException);
                    }
                    return false;
                }
            }, new TimeoutBudget(1, TimeUnit.MINUTES));
        } catch (Exception e) {
            try {
                adapterClient.close();
            } catch (Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }

        log.info("Connection to mqtt adapter succeeded");

        return new MqttAdapterClient(adapterClient);
    }
}
