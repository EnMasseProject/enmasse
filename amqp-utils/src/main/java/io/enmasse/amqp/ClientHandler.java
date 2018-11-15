/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.message.Message;

public interface ClientHandler {
    void onReceiverAttached(String remoteContainerId, String replyTo);
    void onMessage(Message message, Delivery delivery);
    void onTransportError(ErrorCondition condition);
}
