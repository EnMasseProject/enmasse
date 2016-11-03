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

/**
 * Subscription Service (SS) endpoint class
 */
public class SubscriptionServiceEndpoint {

    public SubscriptionServiceEndpoint() {

    }

    public void sendCleanSession(/* Clean session info */) {
        // TODO: send AMQP_SESSION message with clean session info
    }

    public void sendSubscribe(/* Subscribe info */) {
        // TODO: send AMQP_SUBSCRIBE message
    }

    public void sendUnsubscribe(/* Unsubscribe info */) {
        // TODO: send AMQP_UNSUBSCRIBE message
    }

    public void sessionHandler(/* Handler */) {
        // TODO: set handler called when AMQP_SESSION_PRESENT is received
    }

    public void publishHandler(/* Handler */) {
        // TODO: set handler called when AMQP_PUBLISH message is received
    }

    public void subackHandler(/* Handler */) {
        // TODO: set handler called when AMQP_SUBACK message is received
    }

    public void unsubackHandler(/* Handler */) {
        // TODO: set handler called when AMQP_UNSUBACK message is received
    }

    public void open() {
        // TODO:

        // attach receiver link on the $mqtt.to.<client-id> address for receiving messages (from SS)
        // define handler for received messages
        // - AMQP_SESSION_PRESENT after sent AMQP_SESSION -> for writing CONNACK (session-present)
        // - AMQP_SUBACK after sent AMQP_SUBSCRIBE
        // - AMQP_UNSUBACK after sent AMQP_UNSUBSCRIBE
        // - AMQP_PUBLISH for every AMQP published message

        // attach sender link to $mqtt.subscriptionservice
    }

    public void close() {

    }
}
