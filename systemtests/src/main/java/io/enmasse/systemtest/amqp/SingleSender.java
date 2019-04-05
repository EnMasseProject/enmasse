/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.amqp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.qpid.proton.message.Message;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonSender;

public class SingleSender extends AbstractSender<List<ProtonDelivery>> {

    /**
     * Recorded deliveries.
     */
    private List<ProtonDelivery> deliveries;

    /**
     * The message to send.
     */
    private final Message message;

    /**
     * The current state, in a human readable format.
     */
    private String state = "initialized";

    public SingleSender(final AmqpConnectOptions clientOptions,
            final LinkOptions linkOptions,
            final CompletableFuture<Void> connectPromise,
            final CompletableFuture<List<ProtonDelivery>> resultPromise,
            final String containerId, final Message message) {

        super(clientOptions, linkOptions, connectPromise, resultPromise, containerId);

        this.deliveries = new LinkedList<>();
        this.message = message;

    }

    @Override
    protected String getCurrentState() {
        return this.state;
    }

    @Override
    protected void sendMessages(final ProtonConnection connection, final ProtonSender sender) {
        this.state = "ready to send";

        switch (sender.getQoS()) {
            case AT_MOST_ONCE:
                sender.send(this.message);
                this.state = "message sent";
                this.resultPromise.complete(Collections.emptyList());
                break;
            case AT_LEAST_ONCE:
                sender.send(this.message, del -> {
                    this.state = "message disposition received";
                    this.deliveries.add(del);
                    if (del.remotelySettled()) {
                        this.state = "message settled";
                        // complete, sending a copy
                        this.resultPromise.complete(new ArrayList<>(this.deliveries));
                    }
                });
                this.state = "message sent";
                break;
        }
    }

}
