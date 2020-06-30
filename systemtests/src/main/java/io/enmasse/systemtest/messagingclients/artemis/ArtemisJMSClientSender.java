/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients.artemis;

import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;

import java.nio.file.Path;


public class ArtemisJMSClientSender extends ProtonJMSClientSender {

    public ArtemisJMSClientSender(Path logPath) throws Exception {
        super(ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER, logPath);
    }

    public ArtemisJMSClientSender() throws Exception {
        super(ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER, null);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        args = javaBrokerTransformation(args);
        return args;
    }
}
