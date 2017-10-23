/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.config.service.amqp;

import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.Subscriber;
import io.vertx.core.Context;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQPSubscriber implements Subscriber {
    private static final Logger log = LoggerFactory.getLogger(AMQPSubscriber.class);

    private final Context context;
    private final String id;
    private final ObserverKey subscriptionKey;
    private final ProtonSender sender;

    public AMQPSubscriber(Context protonContext, ObserverKey subscriptionKey, ProtonConnection connection, ProtonSender sender) {
        this.context = protonContext;
        this.subscriptionKey = subscriptionKey;
        this.id = connection.getRemoteContainer();
        this.sender = sender;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void resourcesUpdated(Message message) {
        context.runOnContext(h -> {
            log.info("Replying to subscription {} with key {} with payload {}", id, message);
            sender.send(message);
        });
    }
}
