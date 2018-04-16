/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.Endpoint;
import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.SSLSocketFactory;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ClientHandlerBase<T> {

    private final String SERVER_URI_TEMPLATE = "tcp://%s:%s";
    private final String TLS_SERVER_URI_TEMPLATE = "ssl://%s:%s";


    private final Endpoint endpoint;
    private final String topic;
    private final CompletableFuture<T> promise;
    private final MqttConnectOptions options;

    protected IMqttAsyncClient client;

    public ClientHandlerBase(Endpoint endpoint,
                             final MqttConnectOptions options,
                             String topic,
                             CompletableFuture<T> promise) {
        this.endpoint = endpoint;
        this.options = options;
        this.topic = topic;
        this.promise = promise;
    }

    public void start() {

        try {

            final String uri_format = this.options.getSocketFactory() instanceof SSLSocketFactory
                    ? TLS_SERVER_URI_TEMPLATE
                    : SERVER_URI_TEMPLATE;
            this.client =
                    new MqttAsyncClient(String.format(uri_format, this.endpoint.getHost(), this.endpoint.getPort()),
                            UUID.randomUUID().toString());

            this.client.connect(options, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    connectionOpened();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    connectionOpenFailed(throwable);
                }
            });


        } catch (MqttException e) {

            e.printStackTrace();
        }

    }

    public void close() {

        try {

            this.client.disconnect();
            this.client.close();

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public final Endpoint getEndpoint() {
        return endpoint;
    }

    protected abstract void connectionOpened();

    protected abstract void connectionOpenFailed(Throwable throwable);

    public String getTopic() {
        return topic;
    }

    public CompletableFuture<T> getPromise() {
        return promise;
    }
}
