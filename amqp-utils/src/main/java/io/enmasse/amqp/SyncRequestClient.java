/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import org.apache.qpid.proton.message.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface SyncRequestClient {
    void connect(String host, int port, ProtonRequestClientOptions clientOptions, String address, CompletableFuture<Void> connectedPromise);
    String getRemoteContainer();
    String getReplyTo();
    void close();

    Message request(Message message, long timeout, TimeUnit timeUnit);
}
