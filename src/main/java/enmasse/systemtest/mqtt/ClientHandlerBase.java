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

package enmasse.systemtest.mqtt;

import enmasse.systemtest.Endpoint;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.UUID;

public abstract class ClientHandlerBase {

    private final Endpoint endpoint;

    protected org.eclipse.paho.client.mqttv3.MqttClient client;

    public ClientHandlerBase(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void start() {

        try {

            this.client =
                    new MqttClient(String.format("tcp://%s:%s", this.endpoint.getHost(), this.endpoint.getPort()),
                            UUID.randomUUID().toString());

            this.client.connect();
            this.connectionOpened();

        } catch (MqttException e) {

            e.printStackTrace();
        }

    }

    protected abstract void connectionOpened();
}
