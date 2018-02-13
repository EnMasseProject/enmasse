/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients.rhea;

import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.Argument;
import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.ClientType;

import java.util.Arrays;
import java.util.List;

public class RheaClientReceiver extends AbstractClient {
    public RheaClientReceiver() {
        super(ClientType.CLI_RHEA_RECEIVER);
    }

    @Override
    protected void fillAllowedArgs() {
        allowedArgs.add(Argument.CONN_URLS);
        allowedArgs.add(Argument.CONN_RECONNECT);
        allowedArgs.add(Argument.CONN_RECONNECT_INTERVAL);
        allowedArgs.add(Argument.CONN_RECONNECT_LIMIT);
        allowedArgs.add(Argument.CONN_RECONNECT_TIMEOUT);
        allowedArgs.add(Argument.CONN_HEARTBEAT);
        allowedArgs.add(Argument.CONN_SSL);
        allowedArgs.add(Argument.CONN_SSL_CERTIFICATE);
        allowedArgs.add(Argument.CONN_SSL_PRIVATE_KEY);
        allowedArgs.add(Argument.CONN_SSL_PASSWORD);
        allowedArgs.add(Argument.CONN_SSL_TRUST_STORE);
        allowedArgs.add(Argument.CONN_SSL_VERIFY_PEER);
        allowedArgs.add(Argument.CONN_SSL_VERIFY_PEER_NAME);
        allowedArgs.add(Argument.CONN_MAX_FRAME_SIZE);
        allowedArgs.add(Argument.CONN_WEB_SOCKET);
        allowedArgs.add(Argument.CONN_PROPERTY);

        allowedArgs.add(Argument.LINK_DURABLE);
        allowedArgs.add(Argument.LINK_AT_MOST_ONCE);
        allowedArgs.add(Argument.LINK_AT_LEAST_ONCE);
        allowedArgs.add(Argument.CAPACITY);

        allowedArgs.add(Argument.LOG_LIB);
        allowedArgs.add(Argument.LOG_STATS);
        allowedArgs.add(Argument.LOG_MESSAGES);

        allowedArgs.add(Argument.BROKER);
        allowedArgs.add(Argument.ADDRESS);
        allowedArgs.add(Argument.COUNT);
        allowedArgs.add(Argument.CLOSE_SLEEP);
        allowedArgs.add(Argument.TIMEOUT);
        allowedArgs.add(Argument.DURATION);

        allowedArgs.add(Argument.SELECTOR);
        allowedArgs.add(Argument.RECV_BROWSE);
        allowedArgs.add(Argument.ACTION);
        allowedArgs.add(Argument.PROCESS_REPLY_TO);
        allowedArgs.add(Argument.RECV_LISTEN);
        allowedArgs.add(Argument.RECV_LISTEN_PORT);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = basicBrokerTransformation(args);
        return args;
    }

    @Override
    protected List<String> transformExecutableCommand(String executableCommand) {
        return Arrays.asList(executableCommand);
    }
}
