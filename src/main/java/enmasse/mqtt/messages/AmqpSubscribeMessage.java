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

package enmasse.mqtt.messages;

import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an AMQP_SUBSCRIBE message
 */
public class AmqpSubscribeMessage {

    public static final String SUBJECT = "subscribe";

    private static final String TOPICS_KEY = "topics";
    private static final String DESIDERED_RCV_SETTLE_MODE_KEY = "desidered-rcv-settle-modes";

    private String clientId;
    private List<String> topics;
    private List<Integer> qos;

    /**
     * Constructor
     *
     * @param clientId  client identifier
     * @param topics    topics to subscribe
     * @param qos   qos levels for topics to subscribe
     */
    public AmqpSubscribeMessage(String clientId, List<String> topics, List<Integer> qos) {

        this.clientId = clientId;
        this.topics = topics;
        this.qos = qos;
    }

    /**
     * Return an AMQP_SUBSCRIBE message from the raw AMQP one
     *
     * @param message   raw AMQP message
     * @return  AMQP_SUBSCRIBE message
     */
    public static AmqpSubscribeMessage from(Message message) {

        // TODO:
        return new AmqpSubscribeMessage(null, null, null);
    }

    /**
     * Return a raw AMQP message
     *
     * @return
     */
    public Message toAmqp() {

        Message message = ProtonHelper.message();

        message.setSubject(SUBJECT);

        message.setReplyTo(String.format(AmqpCommons.AMQP_CLIENT_ADDRESS_TEMPLATE, this.clientId));

        List<ReceiverSettleMode> desideredRcvSettleModes = this.qos.stream().map(qos -> AmqpHelper.toReceiverSettleMode(qos)).collect(Collectors.toList());

        Map<String, List<?>> map = new HashMap<>();
        map.put(TOPICS_KEY, this.topics);
        map.put(DESIDERED_RCV_SETTLE_MODE_KEY, desideredRcvSettleModes);

        message.setBody(new AmqpValue(map));

        return message;
    }

    /**
     * Client identifier
     * @return
     */
    public String clientId() {
        return this.clientId;
    }

    /**
     * Topics to subscribe
     * @return
     */
    public List<String> topics() {
        return this.topics;
    }

    /**
     * QoS levels for topics to subscribe
     * @return
     */
    public List<Integer> qos() {
        return this.qos;
    }

}
