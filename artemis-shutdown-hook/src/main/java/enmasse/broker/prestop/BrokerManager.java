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
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;

import java.util.Set;

/**
 * A client for retrieving and invoking actions against Artemis.
 */
public class BrokerManager {
    private final Endpoint endpoint;
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
        this.endpoint = mgmtEndpoint;
    }

    public long getQueueMessageCount(String queueName) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putAttribute(message, "core.queue." + queueName, "messageCount");
        session.start();
        ClientMessage reply = requestor.request(message);
        Object count = (Object) ManagementHelper.getResult(reply);
        session.stop();
        System.out.println("Object: " + count.toString());
        return (Long)count;
    }

    public Object[] listQueues() throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "core.server", "getQueueNames");
        session.start();
        ClientMessage reply = requestor.request(message);
        Object[] lists = (Object[]) ManagementHelper.getResult(reply);
        session.stop();
        return lists;
    }

    public Object[] listTopics() throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "jms.server", "getTopicNames");
        session.start();
        ClientMessage reply = requestor.request(message);
        Object [] names = (Object[])ManagementHelper.getResult(reply);
        session.stop();
        return names;
    }

    public boolean createTopic(String name) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "jms.server", "createTopic", name);
        session.start();
        ClientMessage reply = requestor.request(message);
        boolean retVal = (boolean)ManagementHelper.getResult(reply);
        session.stop();
        return retVal;
    }


    public String listConsumers(String connectionId) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "core.server", "listConsumersAsJSON", connectionId);
        session.start();
        ClientMessage reply = requestor.request(message);
        String retVal = (String)ManagementHelper.getResult(reply);
        session.stop();
        return retVal;
    }

    public boolean closeConnections(String address) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "jms.server", "destroyTopic", address, true);
        session.start();
        ClientMessage reply = requestor.request(message);
        Object o = (Object)ManagementHelper.getResult(reply);
        System.out.println("O type: " + o.getClass() + "  value: " + o.toString());
        session.stop();
        return true;
    }

    public String listAllSubscriptions(String address) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, address, "listAllSubscriptionsAsJSON");
        session.start();
        ClientMessage reply = requestor.request(message);
        String retVal = (String)ManagementHelper.getResult(reply);
        session.stop();
        return retVal;
    }

    public void shutdownBroker() throws Exception {
        System.out.println("Shutting down");
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "core.server", "forceFailover");
        session.start();
        requestor.request(message);
        session.stop();
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public String listConnections() throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "core.server", "listConnectionsAsJSON");
        session.start();
        ClientMessage reply = requestor.request(message);
        String retVal = (String)ManagementHelper.getResult(reply);
        session.stop();
        return retVal;
    }

    public void closeSubscriptions(String address, Set<Subscription> subscriptionList) throws Exception {
        for (Subscription sub : subscriptionList) {
            if (sub.isDurable()) {
                System.out.println("CLOSING SUB " + sub.getName());
                ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
                ClientMessage message = session.createMessage(false);
                ManagementHelper.putOperationInvocation(message, address, "dropDurableSubscription", sub.getClientId(), sub.getName());
                session.start();
                requestor.request(message);
                session.stop();
            }
        }
    }
}
