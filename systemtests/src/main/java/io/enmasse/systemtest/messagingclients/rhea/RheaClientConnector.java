/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients.rhea;

import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class RheaClientConnector extends AbstractClient {
    public RheaClientConnector() throws Exception {
        super(ClientType.CLI_RHEA_CONNECTOR);
    }

    public RheaClientConnector(Path logPath) throws Exception {
        super(ClientType.CLI_RHEA_CONNECTOR, logPath);
    }

    @Override
    protected void fillAllowedArgs() {
        allowedArgs.add(ClientArgument.CONN_URLS);
        allowedArgs.add(ClientArgument.CONN_RECONNECT);
        allowedArgs.add(ClientArgument.CONN_RECONNECT_INTERVAL);
        allowedArgs.add(ClientArgument.CONN_RECONNECT_LIMIT);
        allowedArgs.add(ClientArgument.CONN_RECONNECT_TIMEOUT);
        allowedArgs.add(ClientArgument.CONN_HEARTBEAT);
        allowedArgs.add(ClientArgument.CONN_SSL);
        allowedArgs.add(ClientArgument.CONN_SSL_CERTIFICATE);
        allowedArgs.add(ClientArgument.CONN_SSL_PRIVATE_KEY);
        allowedArgs.add(ClientArgument.CONN_SSL_PASSWORD);
        allowedArgs.add(ClientArgument.CONN_SSL_TRUST_STORE);
        allowedArgs.add(ClientArgument.CONN_SSL_VERIFY_PEER);
        allowedArgs.add(ClientArgument.CONN_SSL_VERIFY_PEER_NAME);
        allowedArgs.add(ClientArgument.CONN_MAX_FRAME_SIZE);
        allowedArgs.add(ClientArgument.CONN_WEB_SOCKET);
        allowedArgs.add(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS);
        allowedArgs.add(ClientArgument.CONN_PROPERTY);

        allowedArgs.add(ClientArgument.LINK_DURABLE);
        allowedArgs.add(ClientArgument.LINK_AT_MOST_ONCE);
        allowedArgs.add(ClientArgument.LINK_AT_LEAST_ONCE);
        allowedArgs.add(ClientArgument.CAPACITY);

        allowedArgs.add(ClientArgument.LOG_LIB);
        allowedArgs.add(ClientArgument.LOG_STATS);

        allowedArgs.add(ClientArgument.BROKER);
        allowedArgs.add(ClientArgument.ADDRESS);
        allowedArgs.add(ClientArgument.TIMEOUT);
        allowedArgs.add(ClientArgument.COUNT);
        allowedArgs.add(ClientArgument.OBJECT_CONTROL);
        allowedArgs.add(ClientArgument.SENDER_COUNT);
        allowedArgs.add(ClientArgument.RECEIVER_COUNT);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        args = basicBrokerTransformation(args);
        args.put(ClientArgument.LOG_LIB, "TRANSPORT_FRM");
        args.put(ClientArgument.OBJECT_CONTROL, "CESR");
        return args;
    }

    @Override
    protected List<String> transformExecutableCommand(String executableCommand) {
        return Arrays.asList(executableCommand);
    }
}
