package io.enmasse.systemtest.executor.client.openwire;

import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.ClientType;
import io.enmasse.systemtest.executor.client.proton.java.ProtonJMSClientReceiver;

public class OpenwireJMSClientReceiver extends ProtonJMSClientReceiver {
    public OpenwireJMSClientReceiver(){
        this.setClientType(ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args, ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER);
        args = modifySelectorArg(args);
        return args;
    }
}
