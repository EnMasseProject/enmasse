/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AmqpClientFactory {
    private static Logger log = CustomLogger.getLogger();
    private final AddressSpace defaultAddressSpace;
    private final String defaultUsername;
    private final String defaultPassword;
    private final List<AmqpClient> clients = new CopyOnWriteArrayList<>();

    public AmqpClientFactory(AddressSpace defaultAddressSpace, UserCredentials credentials) {
        this.defaultAddressSpace = defaultAddressSpace;
        this.defaultUsername = credentials.getUsername();
        this.defaultPassword = credentials.getPassword();
    }

    public void close() throws Exception {
        var clients = new ArrayList<>(this.clients);
        for (final AmqpClient client : clients) {
            client.close();
        }
        log.info("Closed {} clients", clients.size());
        clients.clear();
    }

    public AmqpClient createQueueClient(AddressSpace addressSpace) throws Exception {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, addressSpace);
    }

    public AmqpClient createQueueClient() throws Exception {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createTopicClient(AddressSpace addressSpace) throws Exception {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, addressSpace);
    }

    public AmqpClient createTopicClient() throws Exception {
        return createClient(new TopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createDurableTopicClient() throws Exception {
        return createClient(new DurableTopicTerminusFactory(), ProtonQoS.AT_LEAST_ONCE, defaultAddressSpace);
    }

    public AmqpClient createAddressClient(AddressSpace addressSpace, AddressType addressType) throws Exception {
        switch (addressType) {
            case QUEUE:
            case ANYCAST:
            case SUBSCRIPTION:
                return createQueueClient(addressSpace);
            case TOPIC:
                return createTopicClient(addressSpace);
            case MULTICAST:
                return createBroadcastClient(addressSpace);
        }
        throw new IllegalArgumentException("Unknown type " + addressType);
    }

    public AmqpClient createBroadcastClient(AddressSpace addressSpace) throws Exception {
        return createClient(new QueueTerminusFactory(), ProtonQoS.AT_MOST_ONCE, addressSpace);
    }

    public AmqpClient createBroadcastClient() throws Exception {
        return createBroadcastClient(defaultAddressSpace);
    }

    public AmqpClient createClient(TerminusFactory terminusFactory, ProtonQoS qos, AddressSpace addressSpace) throws Exception {
        assertNotNull(addressSpace, "Address space is null");
        Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "messaging");
        if (messagingEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "messaging");
            messagingEndpoint = Kubernetes.getInstance().getExternalEndpoint(externalEndpointName + "-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
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
