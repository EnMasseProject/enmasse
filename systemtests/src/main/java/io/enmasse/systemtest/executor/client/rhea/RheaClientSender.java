package io.enmasse.systemtest.executor.client.rhea;

import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.ClientType;

public class RheaClientSender extends AbstractClient {
    public RheaClientSender(){
        super(ClientType.CLI_RHEA_SENDER);
    }
}
