/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients.proton.java;

import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.ClientArgument;
import io.enmasse.systemtest.clients.ClientArgumentMap;
import io.enmasse.systemtest.clients.ClientType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class ProtonJMSClientSender extends AbstractClient {
    public ProtonJMSClientSender() {
        super(ClientType.CLI_JAVA_PROTON_JMS_SENDER);
    }

    public ProtonJMSClientSender(Path logPath) {
        super(ClientType.CLI_JAVA_PROTON_JMS_SENDER, logPath);
    }

    @Override
    protected void fillAllowedArgs() {
        allowedArgs.add(ClientArgument.CONN_RECONNECT);
        allowedArgs.add(ClientArgument.CONN_RECONNECT_INTERVAL);
        allowedArgs.add(ClientArgument.CONN_RECONNECT_LIMIT);
        allowedArgs.add(ClientArgument.CONN_RECONNECT_TIMEOUT);
        allowedArgs.add(ClientArgument.CONN_HEARTBEAT);
        allowedArgs.add(ClientArgument.CONN_SSL_CERTIFICATE);
        allowedArgs.add(ClientArgument.CONN_SSL_PRIVATE_KEY);
        allowedArgs.add(ClientArgument.CONN_SSL_PASSWORD);
        allowedArgs.add(ClientArgument.CONN_SSL_TRUST_STORE);
        allowedArgs.add(ClientArgument.CONN_SSL_VERIFY_PEER);
        allowedArgs.add(ClientArgument.CONN_SSL_VERIFY_PEER_NAME);
        allowedArgs.add(ClientArgument.CONN_MAX_FRAME_SIZE);
        allowedArgs.add(ClientArgument.CONN_ASYNC_ACKS);
        allowedArgs.add(ClientArgument.CONN_ASYNC_SEND);
        allowedArgs.add(ClientArgument.CONN_AUTH_MECHANISM);
        allowedArgs.add(ClientArgument.CONN_AUTH_SASL);
        allowedArgs.add(ClientArgument.CONN_CLIENT_ID);
        allowedArgs.add(ClientArgument.CONN_CLOSE_TIMEOUT);
        allowedArgs.add(ClientArgument.CONN_CONN_TIMEOUT);
        allowedArgs.add(ClientArgument.CONN_DRAIN_TIMEOUT);
        allowedArgs.add(ClientArgument.CONN_SSL_TRUST_ALL);
        allowedArgs.add(ClientArgument.CONN_SSL_VERIFY_HOST);

        allowedArgs.add(ClientArgument.TX_SIZE);
        allowedArgs.add(ClientArgument.TX_ACTION);
        allowedArgs.add(ClientArgument.TX_ENDLOOP_ACTION);

        allowedArgs.add(ClientArgument.LINK_DURABLE);
        allowedArgs.add(ClientArgument.LINK_AT_MOST_ONCE);
        allowedArgs.add(ClientArgument.LINK_AT_LEAST_ONCE);
        allowedArgs.add(ClientArgument.CAPACITY);

        allowedArgs.add(ClientArgument.LOG_LIB);
        allowedArgs.add(ClientArgument.LOG_STATS);
        allowedArgs.add(ClientArgument.LOG_MESSAGES);

        allowedArgs.add(ClientArgument.BROKER);
        allowedArgs.add(ClientArgument.ADDRESS);
        allowedArgs.add(ClientArgument.USERNAME);
        allowedArgs.add(ClientArgument.PASSWORD);
        allowedArgs.add(ClientArgument.COUNT);
        allowedArgs.add(ClientArgument.CLOSE_SLEEP);
        allowedArgs.add(ClientArgument.TIMEOUT);
        allowedArgs.add(ClientArgument.DURATION);

        allowedArgs.add(ClientArgument.MSG_ID);
        allowedArgs.add(ClientArgument.MSG_GROUP_ID);
        allowedArgs.add(ClientArgument.MSG_GROUP_SEQ);
        allowedArgs.add(ClientArgument.MSG_REPLY_TO_GROUP_ID);
        allowedArgs.add(ClientArgument.MSG_SUBJECT);
        allowedArgs.add(ClientArgument.MSG_REPLY_TO);
        allowedArgs.add(ClientArgument.MSG_PROPERTY);
        allowedArgs.add(ClientArgument.MSG_DURABLE);
        allowedArgs.add(ClientArgument.MSG_TTL);
        allowedArgs.add(ClientArgument.MSG_PRIORITY);
        allowedArgs.add(ClientArgument.MSG_CORRELATION_ID);
        allowedArgs.add(ClientArgument.MSG_USER_ID);
        allowedArgs.add(ClientArgument.MSG_CONTENT_TYPE);
        allowedArgs.add(ClientArgument.MSG_CONTENT);
        allowedArgs.add(ClientArgument.MSG_CONTENT_LIST_ITEM);
        allowedArgs.add(ClientArgument.MSG_CONTENT_MAP_ITEM);
        allowedArgs.add(ClientArgument.MSG_CONTENT_FROM_FILE);
        allowedArgs.add(ClientArgument.MSG_ANNOTATION);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        args = javaBrokerTransformation(args);
        args.put(ClientArgument.LOG_LIB, "trace");
        return args;
    }

    @Override
    protected List<String> transformExecutableCommand(String executableCommand) {
        return Arrays.asList("java", "-jar", executableCommand, "sender");
    }
}
