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

import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;

/**
 * AMQP helper class
 */
public class AmqpHelper {

    /**
     * Map QoS level to sender settle mode
     *
     * @param qos   qos level
     * @return  sender settle mode
     */
    public static SenderSettleMode toSenderSettleMode(int qos) {

        switch (qos) {
            case 0:
                return SenderSettleMode.SETTLED;
            case 1:
            case 2:
                return SenderSettleMode.UNSETTLED;
            default:
                throw new IllegalArgumentException("QoS illegal value");
        }
    }

    /**
     * Map QoS level to receiver settle mode
     *
     * @param qos   qos level
     * @return  receiver settle mode
     */
    public static ReceiverSettleMode toReceiverSettleMode(int qos) {

        switch (qos) {
            case 0:
            case 1:
                return ReceiverSettleMode.FIRST;
            case 2:
                return ReceiverSettleMode.SECOND;
            default:
                throw new IllegalArgumentException("QoS illegal value");
        }
    }
}
