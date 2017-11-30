package io.enmasse.systemtest.executor.client.artemis;

import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.ClientType;
import io.enmasse.systemtest.executor.client.proton.java.ProtonJMSClientReceiver;

public class ArtemisJMSClientReceiver extends ProtonJMSClientReceiver {
    public ArtemisJMSClientReceiver(){
        this.setClientType(ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args, ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER);
        args = modifySelectorArg(args);
        return args;
    }
}
