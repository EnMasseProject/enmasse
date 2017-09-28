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
 *
 *
 */

package io.enmasse.queue.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestConfigServ extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(TestConfigServ.class.getName());
    private ProtonServer server;
    private final int listenPort;
    private final List<ProtonSender> senderList = new ArrayList<>();
    private volatile Message currentConfig;

    public TestConfigServ(int listenPort) {
        this.listenPort = listenPort;
        deployConfig("{}");
    }

    public void deployConfig(String payload) {
        Message message = Message.Factory.create();
        message.setBody(new AmqpValue(payload));
        message.setContentType("application/json");
        currentConfig = message;
        sendConfig();
    }

    @Override
    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.openHandler(conn -> {
                log.info("Connection opened");
            }).closeHandler(conn -> {
                log.info("Connection closed");
                connection.close();
                connection.disconnect();
            }).disconnectHandler(protonConnection -> {
                log.info("Disconnected");
                connection.disconnect();
            }).sessionOpenHandler(ProtonSession::open);

            connection.senderOpenHandler(sender -> {
                senderList.add(sender);
                sendConfig(sender);
                sender.open();
            });

            connection.open();

        }).listen(listenPort);
    }

    private void sendConfig() {
        log.info("Sending config to all");
        for (ProtonSender sender : senderList) {
            sendConfig(sender);
        }
    }

    private void sendConfig(ProtonSender sender) {
        vertx.runOnContext(h -> sender.send(currentConfig));
    }

    @Override
    public void stop() {
        server.close();
    }

    public int getPort() {
        if (server == null) {
            return 0;
        } else {
            return server.actualPort();
        }
    }
}
