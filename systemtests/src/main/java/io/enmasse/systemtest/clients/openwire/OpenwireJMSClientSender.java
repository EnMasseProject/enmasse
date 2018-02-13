package io.enmasse.systemtest.clients.openwire;

import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.ClientType;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientSender;


public class OpenwireJMSClientSender extends ProtonJMSClientSender {
    public OpenwireJMSClientSender() {
        this.setClientType(ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args);
        return args;
    }
}
