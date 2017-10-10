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
package io.enmasse.systemtest.amqp;

import enmasse.systemtest.*;
import io.enmasse.systemtest.*;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AmqpClientFactory {
    private final OpenShift openShift;
    private final Environment environment;
    private final String defaultAddressSpace;
    private final String defaultUsername;
    private final String defaultPassword;
    private final List<AmqpClient> clients = new ArrayList<>();

    public AmqpClientFactory(OpenShift openShift, Environment environment, String defaultAddressSpace, String defaultUsername, String defaultPassword) {
        this.openShift = openShift;
        this.environment = environment;
        this.defaultAddressSpace = defaultAddressSpace;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    public void close() throws Exception {
        for (AmqpClient client : clients) {
            client.close();
        }
        clients.clear();
    }

    public AmqpClient createQueueClient(String addressSpace) throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, addressSpace);
    }

    public AmqpClient createQueueClient() throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createTopicClient() throws UnknownHostException, InterruptedException {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createDurableTopicClient() throws UnknownHostException, InterruptedException {
        return createClient(new DurableTopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createBroadcastClient() throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_MOST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createClient(TerminusFactory terminusFactory, ProtonQoS qos, String addressSpace) throws UnknownHostException, InterruptedException {
        if (environment.useTLS()) {
            Endpoint messagingEndpoint = openShift.getRouteEndpoint(addressSpace, "messaging");
            Endpoint clientEndpoint;
            ProtonClientOptions clientOptions = new ProtonClientOptions();
            clientOptions.setSsl(true);
            clientOptions.setTrustAll(true);
            clientOptions.setHostnameVerificationAlgorithm("");

            if (TestUtils.resolvable(messagingEndpoint)) {
                clientEndpoint = messagingEndpoint;
            } else {
                clientEndpoint = new Endpoint("localhost", 443);
                clientOptions.setSniServerName(messagingEndpoint.getHost());
            }
            Logging.log.info("External endpoint: " + clientEndpoint + ", internal: " + messagingEndpoint);

            return createClient(terminusFactory, clientEndpoint, clientOptions, qos);
        } else {
            return createClient(terminusFactory, openShift.getEndpoint(addressSpace, "messaging"), qos);
        }
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, Endpoint endpoint, ProtonQoS qos) {
        return createClient(terminusFactory, endpoint, new ProtonClientOptions(), qos);
    }

    protected AmqpClient createClient(TerminusFactory terminusFactory, Endpoint endpoint, ProtonClientOptions protonOptions, ProtonQoS qos) {
        AmqpConnectOptions connectOptions = new AmqpConnectOptions()
                .setTerminusFactory(terminusFactory)
                .setEndpoint(endpoint)
                .setProtonClientOptions(protonOptions)
                .setQos(qos)
                .setUsername(defaultUsername)
                .setPassword(defaultPassword);
        return createClient(connectOptions);
    }

    protected AmqpClient createClient(AmqpConnectOptions connectOptions) {
        AmqpClient client = new AmqpClient(connectOptions);
        clients.add(client);
        return client;
    }
}
