/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients.openwire;

import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;

import java.nio.file.Path;


public class OpenwireJMSClientSender extends ProtonJMSClientSender {

    public OpenwireJMSClientSender(Path logPath) throws Exception {
        super(ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER, logPath);
    }

    public OpenwireJMSClientSender() throws Exception {
        super(ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER, null);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        args = javaBrokerTransformation(args);
        return args;
    }
}
