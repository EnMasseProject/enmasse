package io.enmasse.systemtest.executor.client.artemis;

import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.ClientType;
import io.enmasse.systemtest.executor.client.proton.java.ProtonJMSClientSender;


public class ArtemisJMSClientSender extends ProtonJMSClientSender {
    public ArtemisJMSClientSender(){
        this.setClientType(ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args, ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER);
        return args;
    }
}
