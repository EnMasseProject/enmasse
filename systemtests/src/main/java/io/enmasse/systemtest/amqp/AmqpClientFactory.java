/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonQoS;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class AmqpClientFactory {
    private static Logger log = CustomLogger.getLogger();
    private final String defaultUsername;
    private final String defaultPassword;
    private final List<AmqpClient> clients = new CopyOnWriteArrayList<>();

    public AmqpClientFactory(UserCredentials credentials) {
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

    private void probeEndpoint(Endpoint messagingEndpoint) throws IOException {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(10_000);
            socket.connect(new InetSocketAddress(messagingEndpoint.getHost(), messagingEndpoint.getPort()));
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

    public AmqpClient createClient(AmqpConnectOptions connectOptions) {
        AmqpClient client = new AmqpClient(connectOptions);
        clients.add(client);
        return client;
    }
}
