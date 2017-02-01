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

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vert.x based MQTT Last Will and Testament service for EnMasse
 */
public class MqttLwt extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLwt.class);

    private String internalServiceHost;
    private int internalServicePort;

    /**
     * Set the address for connecting to the AMQP internal network
     *
     * @param internalServiceHost   address for AMQP connection
     * @return  current MQTT LWT instance
     */
    public MqttLwt setInternalServiceHost(String internalServiceHost) {
        this.internalServiceHost = internalServiceHost;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP internal network
     *
     * @param internalServicePort   port for AMQP connection
     * @return  current MQTT LWT instance
     */
    public MqttLwt setInternalServicePort(int internalServicePort) {
        this.internalServicePort = internalServicePort;
        return this;
    }
}
