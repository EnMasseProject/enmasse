package io.enmasse.systemtest.executor.client.rhea;

import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.ClientType;

public class RheaClientReceiver extends AbstractClient {
    public RheaClientReceiver(){
        super(ClientType.CLI_RHEA_RECEIVER);
    }
}
