/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class Subscriber extends ClientHandlerBase<List<MqttMessage>> {

    private static Logger log = CustomLogger.getLogger();
    private final List<MqttMessage> messages = new ArrayList<>();
    private final int qos;
    private final Predicate<MqttMessage> done;
    private final CountDownLatch connectLatch;

    public Subscriber(Endpoint endpoint,
                      final MqttConnectOptions options,
                      String topic,
                      int qos,
                      Predicate<MqttMessage> done,
                      CompletableFuture<List<MqttMessage>> promise,
                      CountDownLatch connectLatch) {
        super(endpoint, options, topic, promise);
        this.qos = qos;
        this.done = done;
        this.connectLatch = connectLatch;
    }

    @Override
    protected void connectionOpened() {

        try {

            this.client.subscribe(this.getTopic(), this.qos, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {

                    log.info("Subscription response code {}", iMqttToken.getGrantedQos()[0]);

                    if (iMqttToken.getGrantedQos()[0] == 0x80) {
                        getPromise().completeExceptionally(new RuntimeException("Subscription refused"));
                    }

                    connectLatch.countDown();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {

                    log.info("Subscribe to '{}' failed: {}", iMqttToken.getTopics()[0], throwable.getMessage());
                    getPromise().completeExceptionally(throwable);
                    connectLatch.countDown();
                }
            }, (topic, message) -> {
                log.info("Arrived message-id {}", message.getId());
                messages.add(message);
                if (done.test(message)) {

                    // NOTE : Eclipse Paho doesn't allow to call "disconnect" from a callback
                    //        an exception is raised for that !
                    /* if (this.client.isConnected()) {
                        this.client.disconnect();
                    } */
                    getPromise().complete(messages);
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void connectionOpenFailed(Throwable throwable) {
        log.info("Connection to " + getEndpoint().getHost() + ":" + getEndpoint().getPort() + " failed: " + throwable.getMessage());
        getPromise().completeExceptionally(throwable);
        connectLatch.countDown();
    }
}
