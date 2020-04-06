/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients.artemis;

import java.nio.file.Path;

import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;

public class ArtemisJMSClientReceiver extends ProtonJMSClientReceiver {

    public ArtemisJMSClientReceiver(Path logsPath) throws Exception {
        super(ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER, logsPath);
    }

    public ArtemisJMSClientReceiver() throws Exception {
        super(ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER, null);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        args = javaBrokerTransformation(args);
        args = modifySelectorArg(args);
        return args;
    }
}
