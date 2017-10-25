package io.enmasse.systemtest.executor.client;

import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;

//TODO: Remove this file after dev is completed
public class ClientExecutionUnitTest {

    public static void main(String[] args){
        RheaClientSender sender = new RheaClientSender();
        RheaClientReceiver receiver = new RheaClientReceiver();

        sender.setArguments("--log-msgs", "json", "--count", "10");
        receiver.setArguments("--log-msgs", "json", "--count", "10");

        sender.run();
        receiver.run();
        sender.run();
        receiver.run();

        System.exit(0);
    }
}
