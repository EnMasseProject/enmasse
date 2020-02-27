/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents an Artemis broker that may be managed
 */
public class Artemis implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Artemis.class.getName());
    private final long requestTimeoutMillis;
    private final SyncRequestClient syncRequestClient;

    public Artemis(SyncRequestClient syncRequestClient) {
        this(syncRequestClient, 10_000);
    }

    public Artemis(SyncRequestClient syncRequestClient, long requestTimeoutMillis) {
        this.syncRequestClient = syncRequestClient;
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    private Message doOperation(String resource, String operation, Object ... parameters) throws TimeoutException {
        Message message = createOperationMessage(resource, operation);
        Message response = doRequestResponse(message, parameters);
        if (response == null) {
            throw new TimeoutException("Timed out getting response from broker " + syncRequestClient.getRemoteContainer() + " on " + resource + "." + operation + " with parameters: " + Arrays.toString(parameters));
        }
        return response;
    }

    private Message doAttribute(String resource, String attribute, Object ... parameters) throws TimeoutException {
        Message message = createAttributeMessage(resource, attribute);
        Message response = doRequestResponse(message, parameters);
        if (response == null) {
            throw new TimeoutException("Timed out getting response from broker " + syncRequestClient.getRemoteContainer() + " on " + resource + "." + attribute + " with parameters: " + Arrays.toString(parameters));
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
        return syncRequestClient.request(message, timeout, timeUnit);
    }

    private Message createOperationMessage(String resource, String operation) {
        Message message = Message.Factory.create();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("_AMQ_ResourceName", resource);
        properties.put("_AMQ_OperationName", operation);
        message.setApplicationProperties(new ApplicationProperties(properties));
        return message;
    }

    private Message createAttributeMessage(String resource, String attribute) {
        Message message = Message.Factory.create();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("_AMQ_ResourceName", resource);
        properties.put("_AMQ_Attribute", attribute);
        message.setApplicationProperties(new ApplicationProperties(properties));
        return message;
    }

    public void deployQueue(String name, String address) throws TimeoutException {
        log.info("Deploying queue {} with address {} on broker {}", name, address, syncRequestClient.getRemoteContainer());
        doOperation("broker", "deployQueue", address, name, null, false);
    }

    public void createQueue(String name, String address) throws TimeoutException {
        log.info("Creating queue {} with address {} on broker {}", name, address, syncRequestClient.getRemoteContainer());
        doOperation("broker", "createQueue", address, "ANYCAST", name, null, true, -1, false, true);
    }

    public void createConnectorService(String name, Map<String, String> connParams) throws TimeoutException {
        log.info("Creating connector service {} on broker {}", name, syncRequestClient.getRemoteContainer());
        String factoryName = "org.apache.activemq.artemis.integration.amqp.AMQPConnectorServiceFactory";
        doOperation("broker", "createConnectorService", name, factoryName, connParams);
    }

    public void destroyQueue(String name) throws TimeoutException {
        log.info("Destroying queue {} on broker {}", name, syncRequestClient.getRemoteContainer());
        doOperation("broker", "destroyQueue", name, true);
    }

    public void destroyConnectorService(String address) throws TimeoutException {
        doOperation("broker", "destroyConnectorService", address);
        log.info("Destroyed connector service {} on broker {}", address, syncRequestClient.getRemoteContainer());
    }

    public long getNumQueues() throws TimeoutException {
        return getQueueNames().size();
    }

    public long getQueueMessageCount(String queueName) throws TimeoutException {
        log.info("Checking message count for queue {} on broker {}", queueName, syncRequestClient.getRemoteContainer());
        Message response = doAttribute("queue." + queueName, "messageCount");
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        JsonArray json = new JsonArray(payload);
        return json.getLong(0);
    }

    public String getQueueAddress(String queueName) throws TimeoutException {
        log.info("Checking queue address for queue {} on broker {}", queueName, syncRequestClient.getRemoteContainer());
        Message response = doOperation("queue." + queueName, "getAddress");
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        JsonArray json = new JsonArray(payload);
        return json.getString(0);
    }

    public void forceShutdown() throws TimeoutException {
        log.info("Sending forceShutdown to broker {}", syncRequestClient.getRemoteContainer());
        Message request = createOperationMessage("broker", "forceFailover");
        doRequestResponse(10, TimeUnit.SECONDS, request);
    }

    public Set<String> getAddressNames() throws TimeoutException {
        log.info("Retrieving address names for broker {}", syncRequestClient.getRemoteContainer());
        Message response = doOperation("broker", "getAddressNames");

        Set<String> addressNames = new LinkedHashSet<>();
        JsonArray payload = new JsonArray((String)((AmqpValue)response.getBody()).getValue());
        for (int i = 0; i < payload.size(); i++) {
            JsonArray inner = payload.getJsonArray(i);
            for (int j = 0; j < inner.size(); j++) {
                String addressName = inner.getString(j);
                if (!addressName.equals(syncRequestClient.getReplyTo())) {
                    addressNames.add(addressName);
                }
            }
        }
        return addressNames;
    }

    public Set<String> getQueueNames() throws TimeoutException {
        log.info("Retrieving queue names for broker {}", syncRequestClient.getRemoteContainer());
        Message response = doOperation("broker", "getQueueNames");

        Set<String> queues = new LinkedHashSet<>();
        JsonArray payload = new JsonArray((String)((AmqpValue)response.getBody()).getValue());
        for (int i = 0; i < payload.size(); i++) {
            JsonArray inner = payload.getJsonArray(i);
            for (int j = 0; j < inner.size(); j++) {
                String queueName = inner.getString(j);
                if (!queueName.equals(syncRequestClient.getReplyTo())) {
                    queues.add(queueName);
                }
            }
        }
        return queues;
    }

    public void close() throws Exception {
        syncRequestClient.close();
    }

    public void pauseQueue(String queueName) throws TimeoutException {
        log.info("Pausing queue {}", queueName);
        doOperation("queue." + queueName, "pause");
    }

    public void resumeQueue(String queueName) throws TimeoutException {
        log.info("Resuming queue {}", queueName);
        doOperation("queue." + queueName, "resume");
    }

    public void purgeQueue(String queueName) throws TimeoutException {
        log.info("Purging queue {} on broker {}", queueName, syncRequestClient.getRemoteContainer());
        doOperation("queue." + queueName, "removeAllMessages");
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
        log.info("Retrieving conector names for broker {}", syncRequestClient.getRemoteContainer());
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
