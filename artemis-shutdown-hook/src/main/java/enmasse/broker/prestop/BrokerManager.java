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

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;

/**
 * A client for retrieving and invoking actions against Artemis.
 */
public class BrokerManager {
    private final ServerLocator locator;
    private final ClientSessionFactory sessionFactory;
    private final ClientSession session;

    public BrokerManager(Endpoint mgmtEndpoint) throws Exception {
        this.locator = ActiveMQClient.createServerLocator(String.format("tcp://%s:%s", mgmtEndpoint.hostName(), mgmtEndpoint.port()));
        this.sessionFactory = locator.createSessionFactory();
        this.session = sessionFactory.createSession();
    }

    public int getQueueMessageCount(String queueName) throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putAttribute(message, "core.queue." + queueName, "messageCount");
        session.start();
        ClientMessage reply = requestor.request(message);
        Integer count = (Integer)ManagementHelper.getResult(reply);
        session.stop();
        return count;
    }

    public String[] listQueues() throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "core.server", "getQueueNames");
        session.start();
        ClientMessage reply = requestor.request(message);
        String[] lists = (String[]) ManagementHelper.getResult(reply);
        session.stop();
        return lists;
    }

    public String [] listTopics() throws Exception {
        ClientRequestor requestor = new ClientRequestor(session, "jms.queue.activemq.management");
        ClientMessage message = session.createMessage(false);
        ManagementHelper.putOperationInvocation(message, "jms.server", "getTopicNames");
        session.start();
        ClientMessage reply = requestor.request(message);
        String [] names = (String [])ManagementHelper.getResult(reply);
        session.stop();
        return names;
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

}
