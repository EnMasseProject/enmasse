/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.message.Message;

import java.util.List;
import java.util.concurrent.Future;

public interface ReceiverStatus extends AutoCloseable {
    Future<List<Message>> getResult();

    int getNumReceived();
}
