/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients.proton.java;

import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.Argument;
import io.enmasse.systemtest.clients.ArgumentMap;
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
        allowedArgs.add(Argument.CONN_RECONNECT);
        allowedArgs.add(Argument.CONN_RECONNECT_INTERVAL);
        allowedArgs.add(Argument.CONN_RECONNECT_LIMIT);
        allowedArgs.add(Argument.CONN_RECONNECT_TIMEOUT);
        allowedArgs.add(Argument.CONN_HEARTBEAT);
        allowedArgs.add(Argument.CONN_SSL_CERTIFICATE);
        allowedArgs.add(Argument.CONN_SSL_PRIVATE_KEY);
        allowedArgs.add(Argument.CONN_SSL_PASSWORD);
        allowedArgs.add(Argument.CONN_SSL_TRUST_STORE);
        allowedArgs.add(Argument.CONN_SSL_VERIFY_PEER);
        allowedArgs.add(Argument.CONN_SSL_VERIFY_PEER_NAME);
        allowedArgs.add(Argument.CONN_MAX_FRAME_SIZE);
        allowedArgs.add(Argument.CONN_ASYNC_ACKS);
        allowedArgs.add(Argument.CONN_ASYNC_SEND);
        allowedArgs.add(Argument.CONN_AUTH_MECHANISM);
        allowedArgs.add(Argument.CONN_AUTH_SASL);
        allowedArgs.add(Argument.CONN_CLIENT_ID);
        allowedArgs.add(Argument.CONN_CLOSE_TIMEOUT);
        allowedArgs.add(Argument.CONN_CONN_TIMEOUT);
        allowedArgs.add(Argument.CONN_DRAIN_TIMEOUT);
        allowedArgs.add(Argument.CONN_SSL_TRUST_ALL);
        allowedArgs.add(Argument.CONN_SSL_VERIFY_HOST);

        allowedArgs.add(Argument.TX_SIZE);
        allowedArgs.add(Argument.TX_ACTION);
        allowedArgs.add(Argument.TX_ENDLOOP_ACTION);

        allowedArgs.add(Argument.LINK_DURABLE);
        allowedArgs.add(Argument.LINK_AT_MOST_ONCE);
        allowedArgs.add(Argument.LINK_AT_LEAST_ONCE);
        allowedArgs.add(Argument.CAPACITY);

        allowedArgs.add(Argument.LOG_LIB);
        allowedArgs.add(Argument.LOG_STATS);
        allowedArgs.add(Argument.LOG_MESSAGES);

        allowedArgs.add(Argument.BROKER);
        allowedArgs.add(Argument.ADDRESS);
        allowedArgs.add(Argument.USERNAME);
        allowedArgs.add(Argument.PASSWORD);
        allowedArgs.add(Argument.COUNT);
        allowedArgs.add(Argument.CLOSE_SLEEP);
        allowedArgs.add(Argument.TIMEOUT);
        allowedArgs.add(Argument.DURATION);

        allowedArgs.add(Argument.MSG_ID);
        allowedArgs.add(Argument.MSG_GROUP_ID);
        allowedArgs.add(Argument.MSG_GROUP_SEQ);
        allowedArgs.add(Argument.MSG_REPLY_TO_GROUP_ID);
        allowedArgs.add(Argument.MSG_SUBJECT);
        allowedArgs.add(Argument.MSG_REPLY_TO);
        allowedArgs.add(Argument.MSG_PROPERTY);
        allowedArgs.add(Argument.MSG_DURABLE);
        allowedArgs.add(Argument.MSG_TTL);
        allowedArgs.add(Argument.MSG_PRIORITY);
        allowedArgs.add(Argument.MSG_CORRELATION_ID);
        allowedArgs.add(Argument.MSG_USER_ID);
        allowedArgs.add(Argument.MSG_CONTENT_TYPE);
        allowedArgs.add(Argument.MSG_CONTENT);
        allowedArgs.add(Argument.MSG_CONTENT_LIST_ITEM);
        allowedArgs.add(Argument.MSG_CONTENT_MAP_ITEM);
        allowedArgs.add(Argument.MSG_CONTENT_FROM_FILE);
        allowedArgs.add(Argument.MSG_ANNOTATION);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args);
        args.put(Argument.LOG_LIB, "trace");
        return args;
    }

    @Override
    protected List<String> transformExecutableCommand(String executableCommand) {
        return Arrays.asList("java", "-jar", executableCommand, "sender");
    }
}
