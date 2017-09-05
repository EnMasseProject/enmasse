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

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import org.apache.activemq.artemis.api.core.ActiveMQConnectionTimedOutException;
import org.apache.activemq.artemis.api.core.ActiveMQNotConnectedException;
import org.apache.activemq.artemis.api.core.ActiveMQObjectClosedException;
import org.apache.activemq.artemis.api.core.ActiveMQUnBlockedException;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A client for retrieving and invoking actions against Artemis.
 */
public class BrokerManager implements AutoCloseable {
    private final ServerLocator locator;
    private final ClientSessionFactory sessionFactory;
    private final ClientSession session;

    public BrokerManager(Endpoint mgmtEndpoint) {
        try {
            this.locator = ActiveMQClient.createServerLocator(String.format("tcp://%s:%s", mgmtEndpoint.hostname(), mgmtEndpoint.port()));
            this.sessionFactory = locator.createSessionFactory();
            this.session = sessionFactory.createSession();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeOperationWithResult(String resource, String cmd, Object ... args) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, resource, cmd, args);
        session.start();
        ClientMessage reply = requestor.request(message);
        Object retVal = ManagementHelper.getResult(reply);
        session.stop();
        return retVal;
    }

    private void invokeOperation(String resource, String cmd, Object ... args) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, resource, cmd, args);
        session.start();
        requestor.request(message);
        session.stop();
    }

    private Object invokeAttribute(String resource, String attribute) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putAttribute(message, resource, attribute);
        session.start();
        ClientMessage reply = requestor.request(message);
        Object retVal = ManagementHelper.getResult(reply);
        session.stop();
        return retVal;
    }

    public long getQueueMessageCount(String queueName) throws Exception {
        return (Long)invokeAttribute("queue." + queueName, "messageCount");
    }

    public String[] listQueues() throws Exception {
        Object[] response = (Object[]) invokeOperationWithResult("broker", "getQueueNames");
        String[] list = new String[response.length];
        for (int i = 0; i < list.length; i++) {
            list[i] = response[i].toString();
        }
        return list;
    }

    public String getQueueAddress(String queueName) throws Exception {
        return (String) invokeOperationWithResult("queue." + queueName, "getAddress");
    }

    public void shutdownBroker() throws Exception {
        try {
            invokeOperation("broker", "forceFailover");
        } catch (ActiveMQObjectClosedException | ActiveMQConnectionTimedOutException | ActiveMQNotConnectedException |ActiveMQUnBlockedException ignored) {
            System.out.println("Disconnected on shutdown");
        }
    }

    public String listConnections() throws Exception {
        return (String)invokeOperationWithResult("broker", "listConnectionsAsJSON");
    }

    public void destroyQueue(QueueInfo queue) throws Exception {
        invokeOperation("broker", "destroyQueue", queue.getQueueName(), true);
    }


    public void waitUntilEmpty(Collection<String> queues) throws InterruptedException {
        while (true) {
            try {
                long count = 0;
                for (String queue : queues) {
                    count += getQueueMessageCount(queue);
                    System.out.println("Found " + count + " messages in queue " + queue);
                }
                if (count == 0) {
                    break;
                }
            } catch (Exception e) {
                // Retry
                System.out.println("Queue check failed: " + e.getMessage());
            }
            Thread.sleep(2000);
        }
    }

    public void createQueue(String address, String name) throws Exception {
        invokeOperation("broker", "createQueue", address, name);
    }

    @Override
    public void close() throws Exception {
        session.close();
        sessionFactory.close();
        locator.close();
    }

    public void resumeQueue(String queueName) throws Exception {
        invokeOperation("queue." + queueName, "resume");
    }

    public void pauseQueue(String queueName) throws Exception {
        invokeOperation("queue." + queueName, "pause");
    }

    public void destroyConnectorService(String connectorName) throws Exception {
        invokeOperation("broker", "destroyConnectorService", connectorName);
    }

    public void createConnectorService(Endpoint messagingEndpoint, String address) throws Exception {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("host", messagingEndpoint.hostname());
        parameters.put("port", String.valueOf(messagingEndpoint.port()));
        parameters.put("containerId", address);
        parameters.put("groupId", address);
        parameters.put("clientAddress", address);
        parameters.put("sourceAddress", address);
        invokeOperation("broker", "createConnectorService", address, "org.apache.activemq.artemis.integration.amqp.AMQPConnectorServiceFactory", parameters);
    }

    public String[] listDiverts() throws Exception {
        Object[] response = (Object[]) invokeOperationWithResult("broker", "getDivertNames");
        String[] list = new String[response.length];
        for (int i = 0; i < list.length; i++) {
            list[i] = response[i].toString();
        }
        return list;
    }

    public String getDivertRoutingName(String divertName) throws Exception {
        return (String) invokeOperationWithResult("divert." + divertName, "getRoutingName");
    }

    public String getDivertAddress(String divertName) throws Exception {
        return (String) invokeOperationWithResult("divert." + divertName, "getAddress");
    }

    public String getDivertForwardingAddress(String divertName) throws Exception {
        return (String) invokeOperationWithResult("divert." + divertName, "getForwardingAddress");
    }

    public void createDivert(String name, String routingName, String address, String forwardingAddress) throws Exception {
        invokeOperation("broker", "createDivert", name, routingName, address, forwardingAddress, false, null, null);
    }

    public void destroySubscriptions(Set<SubscriptionInfo> subscriptions) throws Exception {
        for (SubscriptionInfo subscription : subscriptions) {
            QueueInfo queueInfo = subscription.getQueueInfo();
            destroyQueue(queueInfo);
            if (subscription.getDivertInfo().isPresent()) {
                destroyDivert(subscription.getDivertInfo().get());
            }
            if (subscription.getConnectorInfo().isPresent()) {
                destroyConnectorService(subscription.getConnectorInfo().get().getName());
            }
        }
    }

    private void destroyDivert(DivertInfo divertInfo) throws Exception {
        invokeOperation("broker", "destroyDivert", divertInfo.getName());
    }
}
