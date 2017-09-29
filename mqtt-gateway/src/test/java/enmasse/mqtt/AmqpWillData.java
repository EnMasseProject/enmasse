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

import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.shareddata.Shareable;

/**
 * Class for bringing AMQP_WILL through verticles in a shared data map
 */
public class AmqpWillData implements Shareable {

    private final String linkName;
    private final AmqpWillMessage will;

    /**
     * Constructor
     *
     * @param linkName AMQP_WILL message related client link name
     * @param will  AMQP_WILL message
     */
    public AmqpWillData(String linkName, AmqpWillMessage will) {
        this.linkName = linkName;
        this.will = will;
    }

    /**
     * AMQP_WILL message related client link name
     * @return
     */
    public String linkName() {
        return this.linkName;
    }

    /**
     * AMQP_WILL message
     * @return
     */
    public AmqpWillMessage will() {
        return this.will;
    }
}
