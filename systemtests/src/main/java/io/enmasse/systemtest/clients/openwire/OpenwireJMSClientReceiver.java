package io.enmasse.systemtest.clients.openwire;

import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.ClientType;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientReceiver;

public class OpenwireJMSClientReceiver extends ProtonJMSClientReceiver {
    public OpenwireJMSClientReceiver() {
        this.setClientType(ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args);
        args = modifySelectorArg(args);
        return args;
    }
}
