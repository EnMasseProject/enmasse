/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents an Artemis broker that may be managed
 */
public class Artemis implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Artemis.class.getName());
    private long requestTimeoutMillis = 10_000;
    private final SyncRequestClient syncRequestClient;
    private static final ObjectMapper mapper = new ObjectMapper();

    public Artemis(SyncRequestClient syncRequestClient) {
        this.syncRequestClient = syncRequestClient;
    }

    private Message doOperation(String resource, String operation, Object ... parameters) throws TimeoutException, JsonProcessingException {
        Message message = createOperationMessage(resource, operation);
        Message response = doRequestResponse(message, parameters);
        if (response == null) {
            throw new TimeoutException("Timed out getting response from broker " + syncRequestClient.getRemoteContainer() + " on " + resource + "." + operation + " with parameters: " + Arrays.toString(parameters));
        }
        return response;
    }

    private Message doAttribute(String resource, String attribute, Object ... parameters) throws TimeoutException, JsonProcessingException {
        Message message = createAttributeMessage(resource, attribute);
        Message response = doRequestResponse(message, parameters);
        if (response == null) {
            throw new TimeoutException("Timed out getting response from broker " + syncRequestClient.getRemoteContainer() + " on " + resource + "." + attribute + " with parameters: " + Arrays.toString(parameters));
        }
        return response;
    }

    private Message doRequestResponse(Message message, Object ... parameters) throws TimeoutException, JsonProcessingException {
        return doRequestResponse(requestTimeoutMillis, TimeUnit.MILLISECONDS, message, parameters);
    }

    private Message doRequestResponse(long timeout, TimeUnit timeUnit, Message message, Object ... parameters) throws TimeoutException, JsonProcessingException {
        List<Object> params = new ArrayList<>();
        for (Object param : parameters) {
            if (param == null) {
                params.add(null);
            } else {
                params.add(param);
            }
        }

        message.setBody(new AmqpValue(mapper.writeValueAsString(params)));
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

    public void deployQueue(String name, String address) throws TimeoutException, JsonProcessingException {
        log.info("Deploying queue {} with address {} on broker {}", name, address, syncRequestClient.getRemoteContainer());
        doOperation("broker", "deployQueue", address, name, null, false);
    }

    public void createQueue(String name, String address) throws TimeoutException, JsonProcessingException {
        log.info("Creating queue {} with address {} on broker {}", name, address, syncRequestClient.getRemoteContainer());
        doOperation("broker", "createQueue", address, "ANYCAST", name, null, true, -1, false, true);
    }

    public void createConnectorService(String name, Map<String, String> connParams) throws TimeoutException, JsonProcessingException {
        log.info("Creating connector service {} on broker {}", name, syncRequestClient.getRemoteContainer());
        String factoryName = "org.apache.activemq.artemis.integration.amqp.AMQPConnectorServiceFactory";
        doOperation("broker", "createConnectorService", name, factoryName, connParams);
    }

    public void destroyQueue(String name) throws TimeoutException, JsonProcessingException {
        log.info("Destroying queue {} on broker {}", name, syncRequestClient.getRemoteContainer());
        doOperation("broker", "destroyQueue", name, true);
    }

    public void destroyConnectorService(String address) throws TimeoutException, JsonProcessingException {
        doOperation("broker", "destroyConnectorService", address);
        log.info("Destroyed connector service {} on broker {}", address, syncRequestClient.getRemoteContainer());
    }

    public long getNumQueues() throws TimeoutException, IOException {
        return getQueueNames().size();
    }

    public long getQueueMessageCount(String queueName) throws TimeoutException, IOException {
        log.info("Checking message count for queue {} on broker {}", queueName, syncRequestClient.getRemoteContainer());
        Message response = doAttribute("queue." + queueName, "messageCount");
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        ArrayNode arrayNode = mapper.readValue(payload, ArrayNode.class);
        return arrayNode.get(0).asLong();
    }


    public String getQueueAddress(String queueName) throws TimeoutException, IOException {
        log.info("Checking queue address for queue {} on broker {}", queueName, syncRequestClient.getRemoteContainer());
        Message response = doOperation("queue." + queueName, "getAddress");
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        ArrayNode arrayNode = mapper.readValue(payload, ArrayNode.class);
        return arrayNode.get(0).asText();
    }

    public void forceShutdown() throws TimeoutException, JsonProcessingException {
        log.info("Sending forceShutdown to broker {}", syncRequestClient.getRemoteContainer());
        Message request = createOperationMessage("broker", "forceFailover");
        doRequestResponse(10, TimeUnit.SECONDS, request);
    }

    public Set<String> getQueueNames() throws TimeoutException, IOException {
        log.info("Retrieving queue names for broker {}", syncRequestClient.getRemoteContainer());
        Message response = doOperation("broker", "getQueueNames");

        Set<String> queues = new LinkedHashSet<>();
        ArrayNode payload = mapper.readValue((String)((AmqpValue)response.getBody()).getValue(), ArrayNode.class);
        for (int i = 0; i < payload.size(); i++) {
            ArrayNode inner = (ArrayNode) payload.get(i);
            for (int j = 0; j < inner.size(); j++) {
                String queueName = inner.get(j).asText();
                if (!queueName.equals(syncRequestClient.getReplyTo())) {
                    queues.add(queueName);
                }
            }
        }
        return queues;
    }

    public void close() {
        syncRequestClient.close();
    }

    public void pauseQueue(String queueName) throws TimeoutException, JsonProcessingException {
        log.info("Pausing queue {}", queueName);
        doOperation("queue." + queueName, "pause");
    }

    public void resumeQueue(String queueName) throws TimeoutException, JsonProcessingException {
        log.info("Resuming queue {}", queueName);
        doOperation("queue." + queueName, "resume");
    }

    public Set<String> getDivertNames() throws TimeoutException, IOException {
        log.info("Retrieving divert names");
        Message response = doOperation("broker", "getDivertNames");

        Set<String> diverts = new LinkedHashSet<>();
        ArrayNode payload = mapper.readValue((String)((AmqpValue)response.getBody()).getValue(), ArrayNode.class);

        for (int i = 0; i < payload.size(); i++) {
            ArrayNode inner = (ArrayNode) payload.get(i);
            for (int j = 0; j < inner.size(); j++) {
                diverts.add(inner.get(j).asText());
            }
        }
        return diverts;
    }

    private String doOperationWithStringResult(String resource, String operation, Object ... parameters) throws TimeoutException, IOException {
        Message response = doOperation(resource, operation, parameters);
        String payload = (String) ((AmqpValue)response.getBody()).getValue();
        ArrayNode arrayNode = mapper.readValue(payload, ArrayNode.class);
        return arrayNode.get(0).asText();
    }

    public String getDivertRoutingName(String divertName) throws TimeoutException, IOException {
        log.info("Get routing name for divert {}", divertName);
        return doOperationWithStringResult("divert." + divertName, "getRoutingName");
    }

    public String getDivertAddress(String divertName) throws TimeoutException, IOException {
        log.info("Get address for divert {}", divertName);
        return doOperationWithStringResult("divert." + divertName, "getAddress");
    }

    public String getDivertForwardingAddress(String divertName) throws TimeoutException, IOException {
        log.info("Get forwarding address for divert {}", divertName);
        return doOperationWithStringResult("divert." + divertName, "getForwardingAddress");
    }

    public void createDivert(String divertName, String routingName, String address, String forwardingAddress) throws TimeoutException, JsonProcessingException {
        log.info("Creating divert {}", divertName);
        doOperation("broker", "createDivert", divertName, routingName, address, forwardingAddress, false, null, null);
    }

    public void destroyDivert(String divertName) throws TimeoutException, JsonProcessingException {
        log.info("Destroying divert {}", divertName);
        doOperation("broker", "destroyDivert", divertName);
    }

    public Set<String> getConnectorNames() throws TimeoutException, IOException {
        log.info("Retrieving conector names for broker {}", syncRequestClient.getRemoteContainer());
        Message response = doOperation("broker", "getConnectorServices");

        Set<String> connectors = new LinkedHashSet<>();
        ArrayNode payload = mapper.readValue((String)((AmqpValue)response.getBody()).getValue(), ArrayNode.class);
        for (int i = 0; i < payload.size(); i++) {
            ArrayNode inner = (ArrayNode) payload.get(i);
            for (int j = 0; j < inner.size(); j++) {
                String connector = inner.get(j).asText();
                if (!connector.equals("amqp-connector")) {
                    connectors.add(connector);
                }
            }
        }
        return connectors;
    }
}
