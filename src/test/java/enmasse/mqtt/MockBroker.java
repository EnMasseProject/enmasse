/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpHelper;
import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpTopicSubscription;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock for a "broker like" component
 */
public class MockBroker {

    // topic -> receiver
    private Map<String, ProtonReceiver> receivers;
    // client-id -> sender (to $mqtt.to.<client-id>
    private Map<String, ProtonSender> senders;
    // topic -> client-id lists (subscribers)
    private Map<String, List<String>> subscriptions;
    // topic -> retained message
    private Map<String, AmqpPublishMessage> retained;

    private ProtonConnection connection;

    public MockBroker(ProtonConnection connection) {

        this.connection = connection;

        this.receivers = new HashMap<>();
        this.senders = new HashMap<>();
        this.subscriptions = new HashMap<>();
        this.retained = new HashMap<>();
    }

    public void subscribe(AmqpSubscribeMessage amqpSubscribeMessage) {

        // TODO:

        for (AmqpTopicSubscription amqpTopicSubscription: amqpSubscribeMessage.topicSubscriptions()) {

            // create a receiver for getting messages from the requested topic
            if (!this.receivers.containsKey(amqpTopicSubscription.topic())) {

                ProtonReceiver receiver = this.connection.createReceiver(amqpTopicSubscription.topic());

                receiver
                        .setQoS(amqpTopicSubscription.qos().toProtonQos())
                        .setTarget(receiver.getRemoteTarget())
                        .handler((delivery, message) -> {

                            this.messageHandler(receiver, delivery, message);
                        })
                        .open();

                this.receivers.put(amqpTopicSubscription.topic(), receiver);
            }

            // create a sender to the unique client address for forwarding
            // messages when received on requested topic
            if (!this.senders.containsKey(amqpSubscribeMessage.clientId())) {

                ProtonSender sender = this.connection.createSender(String.format(AmqpHelper.AMQP_CLIENT_ADDRESS_TEMPLATE, amqpSubscribeMessage.clientId()));

                sender
                        .setQoS(amqpTopicSubscription.qos().toProtonQos())
                        .open();

                this.senders.put(amqpSubscribeMessage.clientId(), sender);
            }

            // add the subscription to the requested topic by the client identifier
            if (!this.subscriptions.containsKey(amqpTopicSubscription.topic())) {

                this.subscriptions.put(amqpTopicSubscription.topic(), new ArrayList<>());
            }

            this.subscriptions.get(amqpTopicSubscription.topic()).add(amqpSubscribeMessage.clientId());

        }

    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        String topic = receiver.getSource().getAddress();

        List<String> subscribers = this.subscriptions.get(topic);

        if (subscribers != null) {

            for (String clientId: subscribers) {

                this.senders.get(clientId).send(message);

            }
        }

    }

    public void unsubscribe(AmqpUnsubscribeMessage amqpUnsubscribeMessage) {

        for (String topic: amqpUnsubscribeMessage.topics()) {

            this.subscriptions.get(topic).remove(amqpUnsubscribeMessage.clientId());

            if (this.subscriptions.get(topic).size() == 0) {
                this.subscriptions.remove(topic);
            }
        }
    }

}
