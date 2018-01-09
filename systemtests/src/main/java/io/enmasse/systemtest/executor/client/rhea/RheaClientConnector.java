package io.enmasse.systemtest.executor.client.rhea;

import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.ClientType;

import java.util.Arrays;
import java.util.List;


public class RheaClientConnector extends AbstractClient {
    public RheaClientConnector() {
        super(ClientType.CLI_RHEA_CONNECTOR);
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
        allowedArgs.add(Argument.TIMEOUT);
        allowedArgs.add(Argument.COUNT);
        allowedArgs.add(Argument.OBJECT_CONTROL);
        allowedArgs.add(Argument.SENDER_COUNT);
        allowedArgs.add(Argument.RECEIVER_COUNT);
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
