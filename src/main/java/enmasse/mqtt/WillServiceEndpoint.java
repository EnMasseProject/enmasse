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
 * Will Service (WS) endpoint class
 */
public class WillServiceEndpoint {

    public WillServiceEndpoint() {

    }

    public void open() {
        // TODO:

        // attach sender link to $mqtt.willservice
    }

    public void sendWill(/* Will info */) {
        // TODO: send AMQP_WILL message with will information
    }

    public void clearWill() {
        // TODO: send AMQP_WILL_CLEAR message
    }

    public void close() {

    }
}
