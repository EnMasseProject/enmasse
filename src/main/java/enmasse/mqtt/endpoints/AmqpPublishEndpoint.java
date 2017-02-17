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

package enmasse.mqtt.endpoints;

import enmasse.mqtt.storage.WillMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Publisher endpoint
 */
public class AmqpPublishEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpPublishEndpoint.class);

    private ProtonConnection connection;

    // links for publishing message on topic (topic -> link/senders couple)
    private Map<String, AmqpPublisher> publishers;

    /**
     * Constructor
     *
     * @param connection    ProtonConnection instance
     */
    public AmqpPublishEndpoint(ProtonConnection connection) {
        this.connection = connection;
    }

    /**
     * Open the endpoint, opening the connection
     */
    public void open() {

        this.connection
                .sessionOpenHandler(session -> session.open())
                .open();

        this.publishers = new HashMap<>();
    }

    /**
     * Add a publisher to the endpoint
     *
     * @param address address for which adding the publisher
     */
    public void addPublisher(String address) {

        if (!this.publishers.containsKey(address)) {

            ProtonSender senderQoS1 = this.connection.createSender(address);
            ProtonSender senderQoS2 = this.connection.createSender(address);

            AmqpPublisher publisher = new AmqpPublisher(senderQoS1, senderQoS2);

            this.publishers.put(address, publisher);
        }
    }

    /**
     * Send will message to the attached topic/address
     *
     * @param willMessage   will message to publish
     */
    public void publish(WillMessage willMessage) {

        AmqpPublisher publisher = this.publishers.get(willMessage.topic());

        // use sender for QoS 0/1 messages
        if (willMessage.qos() != MqttQoS.EXACTLY_ONCE) {

            // TODO

        // use sender for QoS 2 messages
        } else {

            // TODO
        }
    }

    /**
     * Close the endpoint, closing the connection
     */
    public void close() {

        // TODO : check what to close other than connection while this class evolves
        if (this.connection != null) {
            this.connection.close();
        }

        if (this.publishers != null) {
            this.publishers.clear();
        }
    }


}
