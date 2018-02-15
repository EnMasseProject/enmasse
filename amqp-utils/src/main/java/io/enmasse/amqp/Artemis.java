/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents an Artemis broker that may be managed
 */
public class Artemis implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Artemis.class.getName());
    private static final int maxRetries = 10;
    private final Context context;
    private final ProtonConnection connection;
    private final ProtonSender sender;
    private final ProtonReceiver receiver;
    private final String replyTo;
    private final BlockingQueue<Message> replies;
    private final String brokerContainerId;
    private long requestTimeoutMillis = 10_000;

    private Artemis(Context context, ProtonConnection connection, ProtonSender sender, ProtonReceiver receiver, String replyTo, BlockingQueue<Message> replies) {
        this.context = context;
        this.connection = connection;
        this.brokerContainerId = connection.getRemoteContainer();
        this.sender = sender;
        this.receiver = receiver;
        this.replyTo = replyTo;
        this.replies = replies;
    }

    public Artemis setRequestTimeout(long timeout, TimeUnit timeUnit) {
        this.requestTimeoutMillis = timeUnit.toMillis(timeout);
        return this;
    }

    public static Future<Artemis> createFromConnection(Vertx vertx, ProtonConnection connection) {
        Future<Artemis> promise = Future.future();
        connection.sessionOpenHandler(ProtonSession::open);
        createSender(vertx, connection, promise, 0);
        return promise;
    }

    public static Future<Artemis> create(Vertx vertx, ProtonClientOptions protonClientOptions, String host, int port) throws InterruptedException {
        Future<Artemis> promise = Future.future();
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(protonClientOptions, host, port, result -> {
            if (result.succeeded()) {
                ProtonConnection connection = result.result();
                createSender(vertx, connection, promise, 0);
                connection.open();
            } else {
                promise.fail(result.cause());
            }
        });
        return promise;
    }

    private static void createSender(Vertx vertx, ProtonConnection connection, Future<Artemis> promise, int retries) {
        ProtonSender sender = connection.createSender("activemq.management");
        sender.openHandler(result -> {
            if (result.succeeded()) {
                createReceiver(vertx, connection, sender, promise, 0);
            } else {
                if (retries > maxRetries) {
                    promise.fail(result.cause());
                } else {
                    log.info("Error creating sender, retries = {}", retries);
                    vertx.setTimer(1000, id -> createSender(vertx, connection, promise, retries + 1));
                }
            }
        });
        sender.open();
    }

    private static void createReceiver(Vertx vertx, ProtonConnection connection, ProtonSender sender, Future<Artemis> promise, int retries) {
        BlockingQueue<Message> replies = new LinkedBlockingDeque<>();
        ProtonReceiver receiver = connection.createReceiver("activemq.management");
        Source source = new Source();
        source.setDynamic(true);
        receiver.setSource(source);
        receiver.openHandler(h -> {
            if (h.succeeded()) {
                promise.complete(new Artemis(vertx.getOrCreateContext(), connection, sender, receiver, h.result().getRemoteSource().getAddress(), replies));
            } else {
                if (retries > maxRetries) {
                    promise.fail(h.cause());
                } else {
                    log.info("Error creating receiver, retries = {}", retries);
                    vertx.setTimer(1000, id -> createReceiver(vertx, connection, sender, promise, retries + 1));
                }
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
    }

    private Message doOperation(String resource, String operation, Object ... parameters) throws TimeoutException {
        Message message = createOperationMessage(resource, operation);
        Message response = doRequestResponse(message, parameters);
        if (response == null) {
            throw new TimeoutException("Timed out getting response from broker " + brokerContainerId + " on " + resource + "." + operation + " with parameters: " + Arrays.toString(parameters));
        }
        return response;
    }

    private Message doAttribute(String resource, String attribute, Object ... parameters) throws TimeoutException {
        Message message = createAttributeMessage(resource, attribute);
        Message response = doRequestResponse(message, parameters);
        if (response == null) {
            throw new TimeoutException("Timed out getting response from broker " + brokerContainerId + " on " + resource + "." + attribute + " with parameters: " + Arrays.toString(parameters));
        }
        return response;
    }

    private Message doRequestResponse(Message message, Object ... parameters) throws TimeoutException {
        return doRequestResponse(requestTimeoutMillis, TimeUnit.MILLISECONDS, message, parameters);
    }

    private Message doRequestResponse(long timeout, TimeUnit timeUnit, Message message, Object ... parameters) throws TimeoutException {
        JsonArray params = new JsonArray();
        for (Object param : parameters) {
            if (param == null) {
                params.addNull();
            } else {
                params.add(param);
            }
        }

        message.setBody(new AmqpValue(Json.encode(params)));
        return sendMessage(message, timeout, timeUnit);
    }

    private Message createOperationMessage(String resource, String operation) {
        Message message = Message.Factory.create();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("_AMQ_ResourceName", resource);
        properties.put("_AMQ_OperationName", operation);
        properties.put("JMSReplyTo", replyTo);
        message.setReplyTo(replyTo);
        message.setApplicationProperties(new ApplicationProperties(properties));
        return message;
    }

    private Message createAttributeMessage(String resource, String attribute) {
        Message message = Message.Factory.create();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("_AMQ_ResourceName", resource);
        properties.put("_AMQ_Attribute", attribute);
        properties.put("JMSReplyTo", replyTo);
        message.setReplyTo(replyTo);
        message.setApplicationProperties(new ApplicationProperties(properties));
        return message;
    }

    private Message sendMessage(Message message, long timeout, TimeUnit timeUnit) {
        context.runOnContext(h -> sender.send(message));
        try {
            Message m = replies.poll(timeout, timeUnit);
            return m;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void deployQueue(String name, String address) throws TimeoutException {
        log.info("Deploying queue {} with address {} on broker {}", name, address, brokerContainerId);
        doOperation("broker", "deployQueue", address, name, null, false);
    }

    public void createQueue(String name, String address) throws TimeoutException {
        log.info("Creating queue {} with address {} on broker {}", name, address, brokerContainerId);
        doOperation("broker", "createQueue", address, "ANYCAST", name, null, true, -1, false, true);
    }

    public void createConnectorService(String name, Map<String, String> connParams) throws TimeoutException {
        log.info("Creating connector service {} on broker {}", name, brokerContainerId);
        String factoryName = "org.apache.activemq.artemis.integration.amqp.AMQPConnectorServiceFactory";
        doOperation("broker", "createConnectorService", name, factoryName, connParams);
    }

    public void destroyQueue(String name) throws TimeoutException {
        log.info("Destroying queue {} on broker {}", name, brokerContainerId);
        doOperation("broker", "destroyQueue", name, true);
    }

    public void destroyConnectorService(String address) throws TimeoutException {
        doOperation("broker", "destroyConnectorService", address);
        log.info("Destroyed connector service {} on broker {}", address, brokerContainerId);
    }

    public long getNumQueues() throws TimeoutException {
        return getQueueNames().size();
    }

    public long getQueueMessageCount(String queueName) throws TimeoutException {
        log.info("Checking message count for queue {} on broker {}", queueName, brokerContainerId);
        Message response = doAttribute("queue." + queueName, "messageCount");
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        JsonArray json = new JsonArray(payload);
        return json.getLong(0);
    }


    public String getQueueAddress(String queueName) throws TimeoutException {
        log.info("Checking queue address for queue {} on broker {}", queueName, brokerContainerId);
        Message response = doOperation("queue." + queueName, "getAddress");
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        JsonArray json = new JsonArray(payload);
        return json.getString(0);
    }

    public void forceShutdown() throws TimeoutException {
        log.info("Sending forceShutdown to broker {}", brokerContainerId);
        Message request = createOperationMessage("broker", "forceFailover");
        doRequestResponse(10, TimeUnit.SECONDS, request);
    }

    public Set<String> getQueueNames() throws TimeoutException {
        log.info("Retrieving queue names for broker {}", brokerContainerId);
        Message response = doOperation("broker", "getQueueNames");

        Set<String> queues = new LinkedHashSet<>();
        JsonArray payload = new JsonArray((String)((AmqpValue)response.getBody()).getValue());
        for (int i = 0; i < payload.size(); i++) {
            JsonArray inner = payload.getJsonArray(i);
            for (int j = 0; j < inner.size(); j++) {
                String queueName = inner.getString(j);
                if (!queueName.equals(replyTo)) {
                    queues.add(queueName);
                }
            }
        }
        return queues;
    }

    public void close() {
        context.runOnContext(id -> connection.close());
    }

    public void pauseQueue(String queueName) throws TimeoutException {
        log.info("Pausing queue {}", queueName);
        doOperation("queue." + queueName, "pause");
    }

    public void resumeQueue(String queueName) throws TimeoutException {
        log.info("Resuming queue {}", queueName);
        doOperation("queue." + queueName, "resume");
    }

    public Set<String> getDivertNames() throws TimeoutException {
        log.info("Retrieving divert names");
        Message response = doOperation("broker", "getDivertNames");

        Set<String> diverts = new LinkedHashSet<>();
        JsonArray payload = new JsonArray((String)((AmqpValue)response.getBody()).getValue());

        for (int i = 0; i < payload.size(); i++) {
            JsonArray inner = payload.getJsonArray(i);
            for (int j = 0; j < inner.size(); j++) {
                diverts.add(inner.getString(j));
            }
        }
        return diverts;
    }

    private String doOperationWithStringResult(String resource, String operation, Object ... parameters) throws TimeoutException {
        Message response = doOperation(resource, operation, parameters);
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        JsonArray json = new JsonArray(payload);
        return json.getString(0);
    }

    public String getDivertRoutingName(String divertName) throws TimeoutException {
        log.info("Get routing name for divert {}", divertName);
        return doOperationWithStringResult("divert." + divertName, "getRoutingName");
    }

    public String getDivertAddress(String divertName) throws TimeoutException {
        log.info("Get address for divert {}", divertName);
        return doOperationWithStringResult("divert." + divertName, "getAddress");
    }

    public String getDivertForwardingAddress(String divertName) throws TimeoutException {
        log.info("Get forwarding address for divert {}", divertName);
        return doOperationWithStringResult("divert." + divertName, "getForwardingAddress");
    }

    public void createDivert(String divertName, String routingName, String address, String forwardingAddress) throws TimeoutException {
        log.info("Creating divert {}", divertName);
        doOperation("broker", "createDivert", divertName, routingName, address, forwardingAddress, false, null, null);
    }

    public void destroyDivert(String divertName) throws TimeoutException {
        log.info("Destroying divert {}", divertName);
        doOperation("broker", "destroyDivert", divertName);
    }

    public Set<String> getConnectorNames() throws TimeoutException {
        log.info("Retrieving conector names for broker {}", brokerContainerId);
        Message response = doOperation("broker", "getConnectorServices");

        Set<String> connectors = new LinkedHashSet<>();
        JsonArray payload = new JsonArray((String)((AmqpValue)response.getBody()).getValue());
        for (int i = 0; i < payload.size(); i++) {
            JsonArray inner = payload.getJsonArray(i);
            for (int j = 0; j < inner.size(); j++) {
                String connector = inner.getString(j);
                if (!connector.equals("amqp-connector")) {
                    connectors.add(connector);
                }
            }
        }
        return connectors;
    }
}
