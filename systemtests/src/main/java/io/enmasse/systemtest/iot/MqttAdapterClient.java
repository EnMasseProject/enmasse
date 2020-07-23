/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.utils.ThrowingConsumer;
import io.vertx.core.buffer.Buffer;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class MqttAdapterClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttAdapterClient.class);

    /**
     * No-op listener instance for QoS 0.
     */
    private static final IMqttActionListener NOOP = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        }
    };

    private final IMqttAsyncClient adapterClient;

    private MqttAdapterClient(final IMqttAsyncClient adapterClient) {
        this.adapterClient = adapterClient;
    }

    @Override
    public void close() throws Exception {
        this.adapterClient.close();
    }

    public boolean sendQos0(final MessageSendTester.Type type, Buffer payload, Duration timeout) {
        return send(0, type.type(), payload, timeout);
    }

    public boolean sendQos1(final MessageSendTester.Type type, Buffer payload, Duration timeout) {
        return send(1, type.type(), payload, timeout);
    }

    public boolean send(int qos, final MessageType type, final Buffer payload, final Duration timeout) {
        return send(qos, type.address(), payload, timeout);
    }

    /**
     * Convert a future to an MQTT listener, depending on the QoS.
     * <br>
     * The method will map a future to the outcome of an MQTT operation, tracked using
     * the generated action listener. The caller must ensure that the action listener instance
     * is being updated with the outcome, only then will the future be completed.
     * <br>
     * If the QoS is zero, the MQTT stack will not update the action listener, and thus this
     * method will return a static (no-op) instance, but also complete the future before returning.
     *
     * @param qos The QoS the messaging will be sent with.
     * @param future The future to handle.
     * @return A new listener instance, never returns {@code null}.
     */
    private static IMqttActionListener toListener(final int qos, final CompletableFuture<?> future) {

        if (qos < 1) {
            future.complete(null);
            return NOOP;
        }

        return new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                future.complete(null);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                future.completeExceptionally(exception);
            }
        };

    }

    public CompletableFuture<?> sendAsync(int qos, final String address, final Buffer payload) {

        var promise = new CompletableFuture<>();

        final MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(qos);

        try {
            this.adapterClient.publish(address, message, null, toListener(qos, promise));
        } catch (MqttException e) {
            promise.completeExceptionally(e);
        }

        return promise;

    }

    public boolean send(int qos, final String address, final Buffer payload, final Duration timeout) {

        try {

            sendAsync(qos, address, payload)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;

        } catch (Exception e) {

            log.info("Failed to send MQTT message", e);
            return false;

        }

    }

    public CompletableFuture<?> subscribe (final String address, final int qos, final BiConsumer<String, MqttMessage> messageHandler) {

        Objects.requireNonNull(messageHandler);

        var promise = new CompletableFuture<>();

        try {
            this.adapterClient.subscribe(address, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    promise.complete(null);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    promise.completeExceptionally(exception);
                }
            }, messageHandler::accept);
        } catch (MqttException e) {
            log.warn("Failed to subscribe to '{}' with QoS: {}", address, qos, e);
            promise.completeExceptionally(e);
        }

        return promise;
    }

    public static MqttAdapterClient create(final Endpoint endpoint, final String deviceId, final PrivateKey key, final X509Certificate certificate)
            throws Exception {

        return create(endpoint, deviceId, builder -> {
            builder.clientCertificate(KeyStoreCreator.from(key, certificate));
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

        // try connecting to MQTT endpoint

        final Instant tryUntil = Instant.now().plus(Duration.ofMinutes(1));
        while (true) {

            try {

                // try connecting
                adapterClient.connect().waitForCompletion(10_000);

                // success -> return result
                log.info("Connection to mqtt adapter succeeded");
                return new MqttAdapterClient(adapterClient);

            } catch (final MqttException mqttException) {

                // something failed ...

                log.info("Failed to connect to MQTT endpoint", mqttException);
                if (Instant.now().isAfter(tryUntil)) {

                    // we ran out of time ...

                    try {
                        // try to close, don't leak resources
                        adapterClient.close();
                    } catch (Exception e) {
                        // add close failure to suppressed list
                        mqttException.addSuppressed(e);
                    }

                    // throw original MQTT exception
                    throw mqttException;
                }

                // let's wait a bit
                Thread.sleep(5_000);

                // any try once more -> goes back to start of while

            }

        }

    }
}
