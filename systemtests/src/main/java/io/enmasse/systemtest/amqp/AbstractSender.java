/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.logs.CustomLogger;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractSender<T> extends ClientHandlerBase<T> {

    private static Logger log = CustomLogger.getLogger();

    public AbstractSender(AmqpConnectOptions clientOptions, LinkOptions linkOptions, CompletableFuture<Void> connectPromise, CompletableFuture<T> resultPromise, String containerId) {
        super(clientOptions, linkOptions, connectPromise, resultPromise, containerId);
    }

    protected abstract String getCurrentState();

    protected abstract void sendMessages(ProtonConnection connection, ProtonSender sender);

    @Override
    public void connectionOpened(ProtonConnection connection) {
        ProtonSender sender = connection.createSender(linkOptions.getTarget().getAddress());
        sender.setTarget(linkOptions.getTarget());
        sender.setQoS(clientOptions.getQos());
        sender.openHandler(result -> {
            if (result.succeeded()) {
                log.info("Sender link '" + sender.getTarget().getAddress() + "' opened, sending messages");
                connectPromise.complete(null);
                sendMessages(connection, sender);
            } else {
                handleError(connection, sender.getRemoteCondition());
            }
        });
        sender.closeHandler(result -> handleError(connection, sender.getRemoteCondition()));
        sender.open();
    }

    @Override
    protected void connectionClosed(ProtonConnection conn) {
        conn.close();
        resultPromise.completeExceptionally(new RuntimeException("Connection closed" + getCurrentState()));
        connectPromise.completeExceptionally(new RuntimeException("Connection closed after " + getCurrentState()));
    }

    @Override
    protected void connectionDisconnected(ProtonConnection conn) {
        conn.close();
        resultPromise.completeExceptionally(new RuntimeException("Connection disconnected after " + getCurrentState()));
        connectPromise.completeExceptionally(new RuntimeException("Connection disconnected after " + getCurrentState()));
    }


}
