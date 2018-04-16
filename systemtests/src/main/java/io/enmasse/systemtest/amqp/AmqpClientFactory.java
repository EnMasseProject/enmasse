/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.*;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AmqpClientFactory {
    private static Logger log = CustomLogger.getLogger();
    private final Kubernetes kubernetes;
    private final Environment environment;
    private final AddressSpace defaultAddressSpace;
    private final String defaultUsername;
    private final String defaultPassword;
    private final List<AmqpClient> clients = new ArrayList<>();

    public AmqpClientFactory(Kubernetes kubernetes, Environment environment, AddressSpace defaultAddressSpace, String defaultUsername, String defaultPassword) {
        this.kubernetes = kubernetes;
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

    public AmqpClient createQueueClient(AddressSpace addressSpace) throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, addressSpace);
    }

    public AmqpClient createQueueClient() throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createTopicClient(AddressSpace addressSpace) throws UnknownHostException, InterruptedException {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, addressSpace);
    }

    public AmqpClient createTopicClient() throws UnknownHostException, InterruptedException {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createDurableTopicClient() throws UnknownHostException, InterruptedException {
        return createClient(new DurableTopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createBroadcastClient(AddressSpace addressSpace) throws UnknownHostException, InterruptedException {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_MOST_ONCE, addressSpace);
    }

    public AmqpClient createBroadcastClient() throws UnknownHostException, InterruptedException {
        return createBroadcastClient(defaultAddressSpace);
    }

    public AmqpClient createClient(TerminusFactory terminusFactory, ProtonQoS qos, AddressSpace addressSpace) throws UnknownHostException, InterruptedException {
        assertNotNull(addressSpace, "Address space is null");
        if (environment.useTLS()) {
            Endpoint messagingEndpoint = addressSpace.getEndpoint("messaging");
            if (messagingEndpoint == null) {
                String externalEndpointName = TestUtils.getExternalEndpointName(addressSpace, "messaging");
                messagingEndpoint = kubernetes.getExternalEndpoint(addressSpace.getNamespace(), externalEndpointName);
            }
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
            log.info("External endpoint: " + clientEndpoint + ", internal: " + messagingEndpoint);

            return createClient(terminusFactory, clientEndpoint, clientOptions, qos);
        } else {
            return createClient(terminusFactory, kubernetes.getEndpoint(addressSpace.getNamespace(), "messaging", "amqps"), qos);
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
