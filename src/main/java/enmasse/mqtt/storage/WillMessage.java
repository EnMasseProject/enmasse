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

package enmasse.mqtt.storage;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;

/**
 * Will information
 */
public class WillMessage {

    private final boolean isRetain;
    private final String topic;
    private final MqttQoS qos;
    private final Buffer payload;

    /**
     * Constructor
     *
     * @param isRetain  will retain flag
     * @param topic will topic
     * @param qos MQTT QoS level
     * @param payload   will message payload
     */
    public WillMessage(boolean isRetain, String topic, MqttQoS qos, Buffer payload) {

        this.isRetain = isRetain;
        this.topic = topic;
        this.qos = qos;
        this.payload = payload;
    }

    /**
     * Will retain flag
     * @return
     */
    public boolean isRetain() {
        return this.isRetain;
    }

    /**
     * Will topic
     * @return
     */
    public String topic() {
        return this.topic;
    }

    /**
     * MQTT QoS level
     * @return
     */
    public MqttQoS qos() {
        return this.qos;
    }

    /**
     * Will message payload
     * @return
     */
    public Buffer payload() {
        return this.payload;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        WillMessage other = (WillMessage)obj;

        if (this.isRetain != other.isRetain()) {
            return false;
        }

        if (!this.topic.equals(other.topic())) {
            return false;
        }

        if (this.qos != other.qos()) {
            return false;
        }

        if (this.payload.length() != other.payload().length()) {
            return false;
        } else {
            return this.byteArraysEqual(this.payload.getBytes(), other.payload().getBytes());
        }
    }

    private boolean byteArraysEqual(byte[] b1, byte[] b2) {

        if (b1.length != b2.length) return false;
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "WillMessage{" +
                "isRetain=" + this.isRetain +
                ", topic=" + this.topic +
                ", qos=" + this.qos +
                ", payload=" + this.payload +
                "}";
    }
}
