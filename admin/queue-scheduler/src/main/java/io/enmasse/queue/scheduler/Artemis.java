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

package io.enmasse.queue.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Vertx;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Represents an Artemis broker that may be managed
 */
public class Artemis implements Broker {
    private static final Logger log = LoggerFactory.getLogger(Artemis.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Vertx vertx;
    private final ProtonSender sender;
    private final String replyTo;
    private final BlockingQueue<Message> replies;
    private final String messagingHost = System.getenv("MESSAGING_SERVICE_HOST");
    private final String messagingPort = System.getenv("MESSAGING_SERVICE_PORT_AMQPS_BROKER");

    public Artemis(Vertx vertx, ProtonSender sender, String replyTo, BlockingQueue<Message> replies) {
        this.vertx = vertx;
        this.sender = sender;
        this.replyTo = replyTo;
        this.replies = replies;
    }

    public static Future<Broker> create(Vertx vertx, ProtonConnection connection) {
        CompletableFuture<Broker> promise = new CompletableFuture<>();
        connection.sessionOpenHandler(ProtonSession::open);
        BlockingQueue<Message> replies = new LinkedBlockingDeque<>();
        ProtonSender sender = connection.createSender("activemq.management");
        sender.openHandler(result -> {
            ProtonReceiver receiver = connection.createReceiver("activemq.management");
            Source source = new Source();
            source.setDynamic(true);
            receiver.setSource(source);
            receiver.openHandler(h -> {
                if (h.succeeded()) {
                    promise.complete(new Artemis(vertx, sender, h.result().getRemoteSource().getAddress(), replies));
                } else {
                    promise.completeExceptionally(h.cause());
                }
            });
            receiver.handler(((protonDelivery, message) -> {
                try {
                    replies.put(message);
                    ProtonHelper.accepted(protonDelivery, true);
                } catch (Exception e) {
                    ProtonHelper.rejected(protonDelivery, true);
                }
            }));
            receiver.open();
        });
        sender.open();
        return promise;
    }

    @Override
    public void deployQueue(String address) {
        Message message = createMessage("deployQueue");
        ArrayNode parameters = mapper.createArrayNode();
        parameters.add(address);
        parameters.add(address);
        parameters.addNull();
        parameters.add(false);

        message.setBody(new AmqpValue(encodeJson(parameters)));
        Message response = doRequest(message);
        if (response == null) {
            log.warn("Timed out getting response from broker");
            return;
        }

        message = createMessage("createConnectorService");
        parameters = mapper.createArrayNode();
        parameters.add(address);
        parameters.add("org.apache.activemq.artemis.integration.amqp.AMQPConnectorServiceFactory");
        ObjectNode connectorParams = parameters.addObject();
        connectorParams.put("host", messagingHost);
        connectorParams.put("port", messagingPort);
        connectorParams.put("containerId", address);
        connectorParams.put("clusterId", address);

        message.setBody(new AmqpValue(encodeJson(parameters)));
        response = doRequest(message);
        if (response == null) {
            log.warn("Timed out getting response from broker");
            return;
        }
        log.info("Deployed queue " + address);
    }

    private Message doRequest(Message message) {
        vertx.runOnContext(h -> sender.send(message));
        try {
            Message m = replies.poll(60, TimeUnit.SECONDS);
            return m;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeJson(ArrayNode parameters) {
        try {
            return mapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public void deleteQueue(String address) {
        Message message = createMessage("destroyQueue");
        ArrayNode parameters = mapper.createArrayNode();
        parameters.add(address);
        parameters.add(true);
        message.setBody(new AmqpValue(encodeJson(parameters)));

        Message response = doRequest(message);
        if (response == null) {
            log.warn("Timed out getting response from broker");
            return;
        }

        message = createMessage("destroyConnectorService");
        parameters = mapper.createArrayNode();
        parameters.add(address);
        message.setBody(new AmqpValue(encodeJson(parameters)));
        response = doRequest(message);
        if (response == null) {
            log.warn("Timed out getting response from broker");
            return;
        }
        log.info("Destroyed queue " + address);
    }

    private Message createMessage(String operation) {
        Message message = Message.Factory.create();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("_AMQ_ResourceName", "broker");
        properties.put("_AMQ_OperationName", operation);
        properties.put("JMSReplyTo", replyTo);
        message.setReplyTo(replyTo);
        message.setApplicationProperties(new ApplicationProperties(properties));
        return message;
    }

    @Override
    public long getNumQueues() {
        return getQueueNames().size();
    }

    @Override
    public Set<String> getQueueNames() {
        Set<String> queues = new LinkedHashSet<>();
        Message message = createMessage("getQueueNames");
        message.setBody(new AmqpValue("[]"));

        // TODO: Make this method less ugly
        Message response = doRequest(message);
        if (response == null) {
            log.warn("Timed out getting response from broker");
            return queues;
        }

        AmqpValue value = (AmqpValue) response.getBody();
        ArrayNode root;
        try {
            root = (ArrayNode) mapper.readTree((String) value.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayNode elements = (ArrayNode) root.get(0);
        for (int i = 0; i < elements.size(); i++) {
            String queueName = elements.get(i).asText();
            if (!queueName.equals(replyTo)) {
                queues.add(queueName);
            }
        }
        return queues;
    }
}
