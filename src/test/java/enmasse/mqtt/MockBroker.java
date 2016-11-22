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

import enmasse.mqtt.messages.AmqpSubscribeMessage;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;

import java.util.List;
import java.util.Map;

/**
 * Mock for a "broker like" component
 */
public class MockBroker {

    private Map<String, ProtonReceiver> receivers;
    private Map<String, List<ProtonSender>> senders;

    private ProtonConnection connection;

    public MockBroker(ProtonConnection connection) {
        this.connection = connection;
    }

    public void subscribe(AmqpSubscribeMessage amqpSubscribeMessage) {

        // TODO:


    }

}
