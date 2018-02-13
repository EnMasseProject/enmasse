package io.enmasse.systemtest.clients.artemis;

import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.ClientType;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientReceiver;

public class ArtemisJMSClientReceiver extends ProtonJMSClientReceiver {
    public ArtemisJMSClientReceiver() {
        this.setClientType(ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args);
        args = modifySelectorArg(args);
        return args;
    }
}
