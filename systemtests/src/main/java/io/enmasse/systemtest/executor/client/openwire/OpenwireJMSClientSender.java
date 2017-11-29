package io.enmasse.systemtest.executor.client.openwire;

import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.ClientType;
import io.enmasse.systemtest.executor.client.proton.java.ProtonJMSClientSender;


public class OpenwireJMSClientSender extends ProtonJMSClientSender {
    public OpenwireJMSClientSender(){
        this.setClientType(ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args, ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER);
        return args;
    }
}
