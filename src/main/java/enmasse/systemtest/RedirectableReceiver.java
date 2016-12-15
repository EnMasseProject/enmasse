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

package enmasse.systemtest;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;

class RedirectableReceiver implements Handler<AsyncResult<ProtonReceiver>> {
    private static final Symbol AMQP_LINK_REDIRECT = Symbol.valueOf("amqp:link:redirect");
    private final Vertx vertx;
    private final ProtonConnection connection;
    private final Source source;
    private final String name;
    private final Handler<AsyncResult<ProtonReceiver>> openHandler;
    private final ProtonMessageHandler messageHandler;
    private ProtonReceiver receiver;

    RedirectableReceiver(Vertx vertx, ProtonConnection connection, Source source, String name,
                         Handler<AsyncResult<ProtonReceiver>> openHandler,
                         ProtonMessageHandler messageHandler) {
        this.vertx = vertx;
        this.connection = connection;
        this.source = source;
        this.name = name;
        this.openHandler = openHandler;
        this.messageHandler = messageHandler;
    }

    ProtonReceiver open() {
        if (receiver != null) {
            receiver = connection.open().createReceiver(source.getAddress(), new ProtonLinkOptions().setLinkName(name));
        } else {
            receiver = connection.createReceiver(source.getAddress(), new ProtonLinkOptions().setLinkName(name));
        }
        receiver.openHandler(openHandler);
        receiver.closeHandler(this);
        receiver.setSource(source);
        receiver.handler(messageHandler);
        receiver.open();
        return receiver;
    }

    ProtonReceiver get() {
        return receiver;
    }

    public void handle(AsyncResult<ProtonReceiver> closed) {
        receiver.close();
        ErrorCondition error = receiver.getRemoteCondition();
        if (error != null && AMQP_LINK_REDIRECT.equals(error.getCondition())) {
            String relocated = (String) error.getInfo().get("address");
            System.out.println("Receiver link redirected to " + relocated);
            source.setAddress(relocated);
            open();
        } else {
            handleReceiverClose(receiver);
        }
    }

    protected void handleReceiverClose(ProtonReceiver receiver) {
        ErrorCondition error = receiver.getRemoteCondition();
        if (error == null || error.getCondition() == null) {
            System.out.println("Receiver link closed without error");
        } else {
            System.out.println("Receiver link closed with " + error);
        }
    }
}
