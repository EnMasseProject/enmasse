/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients.openwire;

import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class OpenwireJMSClientReceiver extends ProtonJMSClientReceiver {

    public OpenwireJMSClientReceiver(Path logPath) throws Exception {
        super(ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER, logPath);
    }

    public OpenwireJMSClientReceiver() throws Exception {
        super(ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER, null);
    }

    @Override
    protected ClientArgumentMap transformArguments(ClientArgumentMap args) {
        args = javaBrokerTransformation(args);
        args = modifySelectorArg(args);
        return args;
    }

    @Override
    public Supplier<Predicate<String>> linkAttachedProbeFactory() {
        return null;
    }

}
