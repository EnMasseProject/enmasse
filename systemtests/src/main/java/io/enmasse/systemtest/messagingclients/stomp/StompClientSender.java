/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients.stomp;

import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class StompClientSender extends AbstractClient {
    public StompClientSender() throws Exception {
        super(ClientType.CLI_STOMP_SENDER);
    }

    public StompClientSender(Path logPath) throws Exception {
        super(ClientType.CLI_STOMP_SENDER, logPath);
    }

    @Override
    protected void fillAllowedArgs() {
        allowedArgs.add(ClientArgument.BROKER);
        allowedArgs.add(ClientArgument.ADDRESS);
        allowedArgs.add(ClientArgument.COUNT);
        allowedArgs.add(ClientArgument.USERNAME);
        allowedArgs.add(ClientArgument.PASSWORD);
        allowedArgs.add(ClientArgument.MSG_CONTENT);
        allowedArgs.add(ClientArgument.DEST_TYPE);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        return args;
    }

    @Override
    protected List<String> transformExecutableCommand(String executableCommand) {
        return Arrays.asList("python", executableCommand, "sender");
    }
}
