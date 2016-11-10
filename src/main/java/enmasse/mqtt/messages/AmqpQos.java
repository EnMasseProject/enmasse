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

import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an AMQP QoS in terms of sender and receiver settle modes
 */
public class AmqpQos {

    private final SenderSettleMode sndSettleMode;
    private final ReceiverSettleMode rcvSettleMode;

    /**
     * Constructor
     *
     * @param sndSettleMode sender settle mode
     * @param rcvSettleMode receiver settle mode
     */
    public AmqpQos(SenderSettleMode sndSettleMode, ReceiverSettleMode rcvSettleMode) {

        this.sndSettleMode = sndSettleMode;
        this.rcvSettleMode = rcvSettleMode;
    }

    /**
     * Convert to a list
     *
     * @return  list with sender and receiver settle modes couple
     */
    public List<UnsignedByte> toList() {

        List<UnsignedByte> list = new ArrayList<>();
        list.add(this.sndSettleMode.getValue());
        list.add(this.rcvSettleMode.getValue());
        return list;
    }

    /**
     * Convert from list to AMQP QoS level
     *
     * @param list  list with sender and receiver settle modes couple
     * @return  AMQP QoS level
     */
    public static AmqpQos toAmqpQos(List<UnsignedByte> list) {

        if ((list.get(0).intValue() > 2 ) || (list.get(1).intValue() > 2))
            throw new IllegalArgumentException("Illegal settle mode");

        // conversion works because value from list used as index in values
        // correspond to enum value in this case
        return new AmqpQos(
                SenderSettleMode.values()[list.get(0).intValue()],
                ReceiverSettleMode.values()[list.get(1).intValue()]);
    }

    /**
     * Convert MQTT QoS level to AMQP QoS level made of sender and receiver settle mode
     *
     * @param mqttQos   MQTT QoS level to map
     * @return  AMQP QoS level
     */
    public static AmqpQos toAmqpQoS(int mqttQos) {

        switch (mqttQos) {
            case 0:
                return new AmqpQos(SenderSettleMode.SETTLED, null);
            case 1:
                return new AmqpQos(SenderSettleMode.UNSETTLED, ReceiverSettleMode.FIRST);
            case 2:
                return new AmqpQos(SenderSettleMode.UNSETTLED, ReceiverSettleMode.SECOND);
            default:
                throw new IllegalArgumentException("Illegal MQTT QoS");
        }
    }

    /**
     * Convert AMQP QoS level (sender and receiver settle mode) to MQTT QoS level
     *
     * @return  MQTT QoS level
     */
    public int toMqttQos() {

        if (this.sndSettleMode == SenderSettleMode.SETTLED) {
            return 0;
        } else if (this.rcvSettleMode == ReceiverSettleMode.FIRST) {
            return 1;
        } else if (this.rcvSettleMode == ReceiverSettleMode.SECOND) {
            return 2;
        } else {
            throw new IllegalArgumentException("Illegal AMQP QoS");
        }
    }

    /**
     * Sender settle mode
     * @return
     */
    public SenderSettleMode sndSettleMode() {
        return this.sndSettleMode;
    }

    /**
     * Receiver settle mode
     * @return
     */
    public ReceiverSettleMode rcvSettleMode() {
        return this.rcvSettleMode;
    }
}
