package io.enmasse.systemtest.executor.client;

import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;

import java.util.HashMap;

//TODO: Remove this file after dev is completed
public class ClientExecutionUnitTest {

    public static void main(String[] args){
        RheaClientSender sender = new RheaClientSender();
        RheaClientReceiver receiver = new RheaClientReceiver();

        HashMap<Argument, String> arguments = new HashMap<>();
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.ADDRESS, "example_queue");

        sender.setArguments(arguments);
        receiver.setArguments(arguments);

        sender.run();
        receiver.run();
        sender.run();
        receiver.run();

        System.exit(0);
    }
}
