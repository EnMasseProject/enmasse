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

package enmasse.queue.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.Assert.assertTrue;


public class TestBroker extends AbstractVerticle {
    private final String id;
    private final String schedulerHost;
    private final int schedulerPort;
    private final Queue<Message> incoming = new ArrayDeque<>();

    public TestBroker(String id, String schedulerHost, int schedulerPort) {
        this.id = id;
        this.schedulerHost = schedulerHost;
        this.schedulerPort = schedulerPort;
    }

    public void start() throws Exception {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(schedulerHost, schedulerPort, openResult -> {
            assertTrue(openResult.succeeded());
            ProtonConnection connection = openResult.result();
            connection.setContainer(id);
            connection.sessionOpenHandler(ProtonSession::open);
            connection.receiverOpenHandler(r -> {
                Source source = new Source();
                source.setAddress("replyaddr");
                r.setSource(source);
                r.handler((protonDelivery, message) -> {
                    incoming.add(message);
                    ProtonHelper.accepted(protonDelivery, true);
                });
                r.open();
            });
            connection.senderOpenHandler(sender -> {
                Source source = new Source();
                source.setAddress("replyaddr");
                sender.setSource(source);
            });
            connection.open();
        });
    }

    public Message receive() {
        return incoming.poll();
    }
}
